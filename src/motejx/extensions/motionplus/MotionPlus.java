/*
 * Copyright 2009 Jan Loesbrock
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package motejx.extensions.motionplus;

import java.util.Collections;
import java.util.Vector;
import javax.swing.event.EventListenerList;
import motej.AbstractExtension;
import motej.Mote;
import motej.event.DataEvent;
import motej.event.DataListener;

public class MotionPlus extends AbstractExtension implements DataListener{

	private EventListenerList listenerList = new EventListenerList();
	
	private boolean yawCalibrated = false;
	private boolean rollCalibrated = false;
	private boolean pitchCalibrated = false;
	private MotionPlusCalibrationData calibrationData;
		
	private Vector<Integer> yawCalibrationData;
	private Vector<Integer> rollCalibrationData;
	private Vector<Integer> pitchCalibrationData;
	
	private Mote mote;
	
//	private MotionPlusEvent lastEvt = null;
		
	@Override
	public void initialize() {
		
		yawCalibrationData = new Vector<Integer>(50,50);
		rollCalibrationData = new Vector<Integer>(50,50);
		pitchCalibrationData = new Vector<Integer>(50,50);
		this.calibrationData = new MotionPlusCalibrationData();
		this.mote.addDataListener(this);
		
//		System.out.println("MotionPlus initilized");
	}

	@Override
	public void parseExtensionData(byte[] extensionData) {
		fireEvent(extensionData);
	}

	@Override
	public void setMote(Mote mote) {
		// TODO Auto-generated method stub
		this.mote = mote;
	}
	
	protected void fireEvent(byte[] data){
		
		int yaw = 	(data[0] & 0xff) ^ ((data[3] & 0xfc) << 6);  // 0xfc -> 11111100
		int roll = 	(data[1] & 0xff) ^ ((data[4] & 0xfc) << 6);
		int pitch = (data[2] & 0xff) ^ ((data[5] & 0xfc) << 6);
				
		boolean yawSlow =  (data[3] & 0x02)>0?true:false;
		boolean pitchSlow = (data[3] & 0x01)>0?true:false; 
		boolean rollSlow = (data[4] & 0x02)>0?true:false;
		boolean extensionConnected = (data[4] & 0x01)>0?true:false;
		
		if (!yawCalibrated || !pitchCalibrated || !rollCalibrated)
		{
			this.calibrate(yaw, roll, pitch);
			return;
		}
		
		
		double calibratedYaw;
		double calibratedRoll;
		double calibratedPitch;
		
		calibratedYaw = ((double) ( yaw - this.calibrationData.getYaw() )) / (yawSlow?20.0:4.0);
		calibratedRoll = ((double) ( roll - this.calibrationData.getRoll() )) / (rollSlow?20.0:4.0);
		calibratedPitch = ((double) ( pitch - this.calibrationData.getPitch() )) / (pitchSlow?20.0:4.0);

//		 
//		if (lastEvt == null)
//		{
//			lastEvt = new MotionPlusEvent(calibratedYaw, calibratedRoll, calibratedPitch); //,yawSlow, pitchSlow, rollSlow, extensionConnected);;
//			return;
//		}
//		
//		double yawDiff = Math.abs(lastEvt.getYawLeftSpeed() - calibratedYaw);
//		double rollDiff = Math.abs(lastEvt.getRollLeftSpeed() -calibratedRoll);
//		double pitchDiff = Math.abs(lastEvt.getPitchDownSpeed() - calibratedPitch);
		
//		calibratedYaw = (yawDiff < filterVal)?lastEvt.getYawLeftSpeed():calibratedYaw;
//		calibratedRoll = (rollDiff < filterVal)?lastEvt.getRollLeftSpeed():calibratedRoll;
//		calibratedPitch = (pitchDiff < filterVal)?lastEvt.getPitchDownSpeed():calibratedPitch;
		
		// noise filter  
		double filterVal = 0.5;		
		calibratedYaw = (Math.abs(calibratedYaw) < filterVal)?0:calibratedYaw;
		calibratedRoll = (Math.abs(calibratedRoll) < filterVal)?0:calibratedRoll;
		calibratedPitch = (Math.abs(calibratedPitch) < filterVal)?0:calibratedPitch;
	
		MotionPlusEvent evt = new MotionPlusEvent(calibratedYaw, calibratedRoll, calibratedPitch); //,yawSlow, pitchSlow, rollSlow, extensionConnected);
//		lastEvt = evt;
		
		MotionPlusListener[] listener = listenerList.getListeners(MotionPlusListener.class);
		
		for (MotionPlusListener l : listener) {
			l.speedChanged(evt);
		}
		
	}

	@Override
	public void dataRead(DataEvent evt) {
		// TODO Auto-generated method stub
	}
	
	public void addMotionPlusEventListener(MotionPlusListener listener)
	{
		listenerList.add(MotionPlusListener.class, listener);
	}
	
	public void removeMotionPlusListener( MotionPlusListener listener)
	{
		listenerList.remove(MotionPlusListener.class, listener);
	}

	private void calibrate(int yaw, int roll, int pitch)
	{
		yawCalibrationData.add(yaw);
		rollCalibrationData.add(roll);
		pitchCalibrationData.add(pitch);
		
		if (yawCalibrationData.size() >= 50 && !yawCalibrated)
		{	
			//Check 50 values
			Vector<Integer> vec = new Vector<Integer>(50);
			vec.addAll( yawCalibrationData.subList( yawCalibrationData.size()-50, yawCalibrationData.size()) );
			Collections.sort(vec);
			int min = vec.firstElement(); 
			int max = vec.lastElement() ;
			int diff = max-min;
			if ( diff <= 75 )
			{
				this.calibrationData.setYaw( (min+max)/2 );
				this.yawCalibrated = true;
			}
		}
		if (rollCalibrationData.size() >= 50 && !rollCalibrated)
		{	
			//Check 50 values
			Vector<Integer> vec = new Vector<Integer>(50);
			vec.addAll( rollCalibrationData.subList( rollCalibrationData.size()-50, rollCalibrationData.size()) );
			Collections.sort(vec);
			int min = vec.firstElement(); 
			int max = vec.lastElement() ;
			int diff = max-min;
			if ( diff <= 75 )
			{
				this.calibrationData.setRoll( (min+max)/2 );
				this.rollCalibrated = true;
			}
		}
		if (pitchCalibrationData.size() >= 50 && !pitchCalibrated)
		{	
			//Check 50 values
			Vector<Integer> vec = new Vector<Integer>(50);
			vec.addAll( pitchCalibrationData.subList( pitchCalibrationData.size()-50, pitchCalibrationData.size()) );
			Collections.sort(vec);
			int min = vec.firstElement(); 
			int max = vec.lastElement() ;
			int diff = max-min;
			if ( diff <= 75 )
			{	
				this.calibrationData.setPitch( (min+max)/2 );
				this.pitchCalibrated = true;
			}
		}
	}
	
	public void newCalibration()
	{
		this.calibrationData = new MotionPlusCalibrationData();
		this.yawCalibrationData = new Vector<Integer>(50,50);
		this.yawCalibrated = false;
		this.rollCalibrationData = new Vector<Integer>(50,50);
		this.rollCalibrated = false;
		this.pitchCalibrationData = new Vector<Integer>(50,50);
		this.pitchCalibrated = false;
	}
}
