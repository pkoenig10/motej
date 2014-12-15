package motej.request;

public class MotionPlusNunchukActivateRequest extends WriteRegisterRequest {

	public MotionPlusNunchukActivateRequest() {
		// Sending 0x05 to 0xa600fe to activate the MotionPlus in Nunchuk
		// pass-through mode
		super(new byte[] { (byte) 0xa6, 0x00, (byte) 0xfe },
				new byte[] { 0x05 });

	}

	@Override
	public byte[] getBytes() {
		return super.getBytes();
	}

}
