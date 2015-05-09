package edu.ycp.robotics;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.*;

import edu.ycp.robotics.PacketParser.State;

public class KobukiRobot {
	
	private boolean stop = false;
	private final PacketParser parser = new PacketParser();
	private final SerialPortHandler handler = new SerialPortHandler();
	private final LinkedBlockingQueue<ByteBuffer> outgoing = new LinkedBlockingQueue<ByteBuffer>();
	private final ScheduledExecutorService executor;
	private final Vector<ScheduledFuture> tasks;
	public static final int MIN_UPDATE_PERIOD = 21; //in ms
		
	private final int WHEELBASE = 230; //in mm
	private int leftEncoder;
	private int rightEncoder;
	private int bumper;
	private int button;
	private int cliff;
	private int battery;
	
	public KobukiRobot(String path) {

		Runnable dataSender = new Runnable() {
			@Override
			public void run() {
                try {
                    while (true) {
                        if (Thread.currentThread().isInterrupted())
                            return;

                        handler.send(outgoing.take());
                    }
                } catch (InterruptedException e) {
                    System.err.println("Interrupted dataSender!");
                    e.printStackTrace();
                }
            }
		};
		
		Runnable dataReceiver = new Runnable() {
			@Override
			public void run() {
                try {
                    while (true) {
                        if (Thread.currentThread().isInterrupted())
                            return;

                        ByteBuffer b = handler.receive();

                        if (b != null) {
                            for (int i = 0; i < b.position(); i++) {
                                if (parser.advance(b.get(i)) == State.VALID) {
                                    updateSensors(parser.getPacket());
                                }
                            }
                        }
                    }
				} catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Interrupted dataReceiver!");
                }
			}
		};
		
		Runnable kobukiThreadManager = new Runnable() {

			@Override
			public void run() {

				if (stop) {
					System.out.println("Shutting down the KobukiRobot.");

                    try {
						baseControl((short) 0, (short) 0);

						// Sleep a bit before killing the running tasks.
						Thread.sleep(3*MIN_UPDATE_PERIOD);
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
					}

                    for(Future<?> currTask : tasks) {  //Properly ends/shuts down all currently active threads
                        currTask.cancel(true);
                        System.out.println(currTask.toString() + " " + currTask.isCancelled());
                    }

                    executor.shutdown();
                } else {
					try {
						Thread.sleep(MIN_UPDATE_PERIOD);
                    } catch (InterruptedException e) {
						Thread.currentThread().interrupt();
                    }
				}
			}
		};
		
		handler.connect(path);
		
		executor = Executors.newScheduledThreadPool(3);
		tasks = new Vector<ScheduledFuture>();
		
		tasks.add(executor.schedule(dataSender, 0, TimeUnit.MILLISECONDS));
        tasks.add(executor.schedule(dataReceiver, 0, TimeUnit.MILLISECONDS));
		tasks.add(executor.scheduleAtFixedRate(kobukiThreadManager, 0, MIN_UPDATE_PERIOD, TimeUnit.MILLISECONDS));
	}

	public void stop() {
        // Stop the Kobuki
        stop = true;
    }
	
	private void updateSensors(byte[] b) {
		
//		for(int i = 0; i < b.length; i++) {
//			System.out.print(b[i] + " ");
//		}
//		
//		System.out.println();
		bumper = b[7]; 
		cliff = b[9];
		leftEncoder = ((b[11] & 0xFF) << 8) | (b[10] & 0xFF);
		rightEncoder = ((b[13] & 0xFF) << 8) | (b[12] & 0xFF);
		button = b[16];
		battery = b[18];


//		System.out.println("ENCODERS: " + leftEncoder + " " + rightEncoder);
	}

	public void setLed(int flag) { 
		//PacketBuilder defines integer tags for leds
		this.outgoing.add(ByteBuffer.wrap(PacketBuilder.ledPacket(flag))); 
	}
	
	public void soundSequence(int sound) { 
		//PacketBuilder defines integer tags for sounds
		this.outgoing.add(ByteBuffer.wrap(PacketBuilder.soundSequencePacket(sound))); 
	}
	
	public void baseControl(short velocity, short radius) throws IOException, InterruptedException { 	
		this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket(velocity, radius))); 
	}
	
	public void control(double v, double w) {
		double epsilon = 0.0001; 
		double radius = 0.0;
		
		if(Math.abs(w) < epsilon) {
			this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket((short) v, (short) radius)));
			return;
		}
		
		radius = v / w;
		
		if(Math.abs(v) < epsilon || Math.abs(radius) <= 1.0) {
			this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket((short) (WHEELBASE * w  / 2.0), (short) 1.0)));
			return;
		}
		
		if(radius > 0.0) {
			this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket((short) ((radius + (WHEELBASE / 2.0)) * w), (short) radius)));
		} else {
			this.outgoing.add(ByteBuffer.wrap(PacketBuilder.baseControlPacket((short) ((radius - (WHEELBASE / 2.0)) * w), (short) radius)));
		}
	}

	public int getRightEncoder() {
		return rightEncoder;
	}

	public int getLeftEncoder() {
		return leftEncoder;
	}
	
	public int getBumper(){
		return bumper;
	}
	
	public int getButton() {
		return button;
	}
	
	public int getCliff() {
		return cliff;
	}
	
	public int getBattery() {
		return battery;
	}
	
	public static void main (String[] args) throws InterruptedException {

		//Small test to make sure everything important is working!

//		KobukiRobot k = new KobukiRobot("COM3");

//		Thread.sleep(3000);

//		k.soundSequence(0);

//		k.setLed(1);
//		Thread.sleep(1000);
//		k.setLed(2);
//		Thread.sleep(1000);
//		k.setLed(3);
//		Thread.sleep(1000);
//		k.setLed(4);
//		k.setLed(0);

//		while(true) {
//
//            if (k.getButton() != 0) {
//                System.out.println("button: " + k.getButton());
//                System.out.println("cliff: " + k.getCliff());
//                System.out.println("battery: " + k.getBattery());
//            }
//
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
}




