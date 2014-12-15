package motejx.extensions.motionplusnunchuk;

import java.awt.Point;
import java.util.Collections;
import java.util.Vector;

import motej.event.AccelerometerEvent;
import motej.event.AccelerometerListener;
import motejx.extensions.motionplus.MotionPlusCalibrationData;
import motejx.extensions.motionplus.MotionPlusEvent;
import motejx.extensions.motionplus.MotionPlusListener;
import motejx.extensions.nunchuk.AnalogStickEvent;
import motejx.extensions.nunchuk.AnalogStickListener;
import motejx.extensions.nunchuk.Nunchuk;
import motejx.extensions.nunchuk.NunchukButtonEvent;
import motejx.extensions.nunchuk.NunchukButtonListener;

public class MotionPlusNunchuk extends Nunchuk {

    private boolean yawCalibrated = false;
    private boolean rollCalibrated = false;
    private boolean pitchCalibrated = false;
    private MotionPlusCalibrationData calibrationData;

    private Vector<Integer> yawCalibrationData;
    private Vector<Integer> rollCalibrationData;
    private Vector<Integer> pitchCalibrationData;

    @Override
    public void initialize() {
        yawCalibrationData = new Vector<Integer>(50, 50);
        rollCalibrationData = new Vector<Integer>(50, 50);
        pitchCalibrationData = new Vector<Integer>(50, 50);
        this.calibrationData = new MotionPlusCalibrationData();

        // initialize
        mote.writeRegisters(new byte[] { (byte) 0xa4, 0x00, 0x40 },
                new byte[] { 0x00 });

        // request calibration data
        mote.readRegisters(new byte[] { (byte) 0xa4, 0x00, 0x30 }, new byte[] {
                0x00, 0x0f });
    }

    @Override
    public void parseExtensionData(byte[] extensionData) {
        int type = (extensionData[5] & 0x02) >> 1;

        switch (type) {
        case 0:
            // Nunchuk data
            fireAnalogStickEvent(extensionData);
            fireAccelerometerEvent(extensionData);
            fireButtonEvent(extensionData);
            break;

        case 1:
            // MotionPlus data
            fireEvent(extensionData);
            break;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void fireAccelerometerEvent(byte[] data) {
        AccelerometerListener<Nunchuk>[] listeners = listenerList
                .getListeners(AccelerometerListener.class);
        if (listeners.length == 0) {
            return;
        }

        // remark: I am currently ignoring the LSB as I do with the LSB of the
        // calibration data.
        // if someone comes up with reliable data, we'll add it, i promise.
        int ax = (data[2] & 0xff);
        int ay = (data[3] & 0xff);
        int az = (data[4] & 0xfe) ^ ((data[5] & 0x80) >> 7);
        AccelerometerEvent<Nunchuk> evt = new AccelerometerEvent<Nunchuk>(this,
                ax, ay, az);
        for (AccelerometerListener<Nunchuk> l : listeners) {
            l.accelerometerChanged(evt);
        }
    }

    @Override
    protected void fireAnalogStickEvent(byte[] data) {
        AnalogStickListener[] listeners = listenerList
                .getListeners(AnalogStickListener.class);
        if (listeners.length == 0) {
            return;
        }

        int sx = data[0] & 0xff;
        int sy = data[1] & 0xff;
        AnalogStickEvent evt = new AnalogStickEvent(this, new Point(sx & 0xff,
                sy & 0xff));
        for (AnalogStickListener l : listeners) {
            l.analogStickChanged(evt);
        }
    }

    @Override
    protected void fireButtonEvent(byte[] data) {
        NunchukButtonListener[] listeners = listenerList
                .getListeners(NunchukButtonListener.class);
        if (listeners.length == 0) {
            return;
        }

        // we invert the original data as the wiimote returns
        // button pressed as nil and thats not that useable.
        int modifiers = ((data[5] & 0x0c) ^ 0x0c) >> 2;

        NunchukButtonEvent evt = new NunchukButtonEvent(this, modifiers);
        for (NunchukButtonListener l : listeners) {
            l.buttonPressed(evt);
        }
    }

    protected void fireEvent(byte[] data) {
        int yaw = (data[0] & 0xff) ^ ((data[3] & 0xfc) << 6);
        int roll = (data[1] & 0xff) ^ ((data[4] & 0xfc) << 6);
        int pitch = (data[2] & 0xff) ^ ((data[5] & 0xfc) << 6);

        boolean yawSlow = (data[3] & 0x02) > 0 ? true : false;
        boolean pitchSlow = (data[3] & 0x01) > 0 ? true : false;
        boolean rollSlow = (data[4] & 0x02) > 0 ? true : false;
        boolean extensionConnected = (data[4] & 0x01) > 0 ? true : false;

        if (!yawCalibrated || !pitchCalibrated || !rollCalibrated) {
            this.calibrate(yaw, roll, pitch);
            return;
        }

        double calibratedYaw;
        double calibratedRoll;
        double calibratedPitch;

        calibratedYaw = (yaw - this.calibrationData.getYaw())
                / (yawSlow ? 20.0 : 4.0);
        calibratedRoll = (roll - this.calibrationData.getRoll())
                / (rollSlow ? 20.0 : 4.0);
        calibratedPitch = (pitch - this.calibrationData.getPitch())
                / (pitchSlow ? 20.0 : 4.0);

        double filterVal = 0.5;
        calibratedYaw = (Math.abs(calibratedYaw) < filterVal) ? 0
                : calibratedYaw;
        calibratedRoll = (Math.abs(calibratedRoll) < filterVal) ? 0
                : calibratedRoll;
        calibratedPitch = (Math.abs(calibratedPitch) < filterVal) ? 0
                : calibratedPitch;

        MotionPlusEvent evt = new MotionPlusEvent(calibratedYaw,
                calibratedRoll, calibratedPitch); // ,yawSlow, pitchSlow,
                                                  // rollSlow,
                                                  // extensionConnected);
        // lastEvt = evt;

        MotionPlusListener[] listener = listenerList
                .getListeners(MotionPlusListener.class);

        for (MotionPlusListener l : listener) {
            l.speedChanged(evt);
        }

    }

    public void addMotionPlusEventListener(MotionPlusListener listener) {
        listenerList.add(MotionPlusListener.class, listener);
    }

    private void calibrate(int yaw, int roll, int pitch) {
        yawCalibrationData.add(yaw);
        rollCalibrationData.add(roll);
        pitchCalibrationData.add(pitch);

        if (yawCalibrationData.size() >= 50 && !yawCalibrated) {
            // Check 50 values
            Vector<Integer> vec = new Vector<Integer>(50);
            vec.addAll(yawCalibrationData.subList(
                    yawCalibrationData.size() - 50, yawCalibrationData.size()));
            Collections.sort(vec);
            int min = vec.firstElement();
            int max = vec.lastElement();
            int diff = max - min;
            if (diff <= 75) {
                this.calibrationData.setYaw((min + max) / 2);
                this.yawCalibrated = true;
            }
        }
        if (rollCalibrationData.size() >= 50 && !rollCalibrated) {
            // Check 50 values
            Vector<Integer> vec = new Vector<Integer>(50);
            vec.addAll(rollCalibrationData.subList(
                    rollCalibrationData.size() - 50, rollCalibrationData.size()));
            Collections.sort(vec);
            int min = vec.firstElement();
            int max = vec.lastElement();
            int diff = max - min;
            if (diff <= 75) {
                this.calibrationData.setRoll((min + max) / 2);
                this.rollCalibrated = true;
            }
        }
        if (pitchCalibrationData.size() >= 50 && !pitchCalibrated) {
            // Check 50 values
            Vector<Integer> vec = new Vector<Integer>(50);
            vec.addAll(pitchCalibrationData.subList(
                    pitchCalibrationData.size() - 50,
                    pitchCalibrationData.size()));
            Collections.sort(vec);
            int min = vec.firstElement();
            int max = vec.lastElement();
            int diff = max - min;
            if (diff <= 75) {
                this.calibrationData.setPitch((min + max) / 2);
                this.pitchCalibrated = true;
            }
        }
    }

    public void newCalibration() {
        this.calibrationData = new MotionPlusCalibrationData();
        this.yawCalibrationData = new Vector<Integer>(50, 50);
        this.yawCalibrated = false;
        this.rollCalibrationData = new Vector<Integer>(50, 50);
        this.rollCalibrated = false;
        this.pitchCalibrationData = new Vector<Integer>(50, 50);
        this.pitchCalibrated = false;
    }

    public void removeMotionPlusListener(MotionPlusListener listener) {
        listenerList.remove(MotionPlusListener.class, listener);
    }

    @Override
    public String toString() {
        return "MotionPlusNunchuk";
    }
}