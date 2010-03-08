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

/*
 * Perhaps it will be better to have one Event for every axis, think about later  
 * @author jan
 *
 */

public class MotionPlusEvent {

	private double yawLeftSpeed;

	private double rollLeftSpeed;

	private double pitchDownSpeed;
	
	private long evtTime;

//	private boolean yawSlow;
//	private boolean pitchSlow;
//	private boolean rollSlow;

//	private boolean extensionConnected;

	public MotionPlusEvent(double yawLeftSpeed, double rollLeftSpeed,
			double pitchDownSpeed){ // boolean yawSlow, boolean pitchSlow, boolean rollSlow, boolean extensionConnected) {
		this.yawLeftSpeed = yawLeftSpeed;
		this.rollLeftSpeed = rollLeftSpeed;
		this.pitchDownSpeed = pitchDownSpeed;
//		this.yawSlow = yawSlow;
//		this.pitchSlow = pitchSlow;
//		this.rollSlow = rollSlow;
//		this.extensionConnected = extensionConnected;
		this.evtTime = System.currentTimeMillis();
	}

	public double getYawLeftSpeed() {
		return yawLeftSpeed;
	}

	public double getRollLeftSpeed() {
		return rollLeftSpeed;
	}

	public double getPitchDownSpeed() {
		return pitchDownSpeed;
	}

	public long getEventTime(){
		return this.evtTime;
	}
//	public boolean isYawSlow() {
//		return yawSlow;
//	}
//
//	public boolean isPitchSlow() {
//		return pitchSlow;
//	}
//
//	public boolean isRollSlow() {
//		return rollSlow;
//	}

//	public boolean isExtensionConnected() {
//		return extensionConnected;
//	}

}
