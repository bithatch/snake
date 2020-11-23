package uk.co.bithatch.snake.lib.backend.openrazer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import uk.co.bithatch.snake.lib.Backend;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.FramePlayer;
import uk.co.bithatch.snake.lib.FramePlayer.FrameListener;
import uk.co.bithatch.snake.lib.Interpolation;
import uk.co.bithatch.snake.lib.KeyFrame;
import uk.co.bithatch.snake.lib.Sequence;

public class MatrixAnimTest {

	public static void main(String[] args) throws Exception {
		Backend be = new OpenRazerBackend();
		be.init();
		Device device = be.getDevices().get(0);
		int[] dim = device.getMatrixSize();

		Sequence seq = new Sequence();

		KeyFrame f1 = new KeyFrame();
		f1.setColor(new int[] { 0xff, 0x00, 0x00 }, dim[0], dim[1]);
		f1.setHoldFor(TimeUnit.SECONDS.toMillis(5));
		seq.add(f1);

		KeyFrame f2 = new KeyFrame();
		f2.setColor(new int[] { 0x00, 0xff, 0x00 }, dim[0], dim[1]);
		f2.setHoldFor(TimeUnit.SECONDS.toMillis(5));
		seq.add(f2);

		KeyFrame f3 = new KeyFrame();
		f3.setColor(new int[] { 0x00, 0x00, 0xff }, dim[0], dim[1]);
		f3.setHoldFor(TimeUnit.SECONDS.toMillis(5));
		seq.add(f3);

		seq.setInterpolation(Interpolation.linear);
		seq.setSpeed(1);
		seq.setFps(25);
		seq.setRepeat(true);

		FramePlayer fp = new FramePlayer(Executors.newSingleThreadScheduledExecutor());
		fp.addListener(new FrameListener() {
			
			@Override
			public void stopped() {
				System.out.println("stopped");
				
			}
			
			@Override
			public void started(Sequence sequence, Device device) {
				System.out.println("started");
				
			}
			
			@Override
			public void frameUpdate(KeyFrame frame, int[][][] rgb, float fac, long frameNumber) {
			}

			@Override
			public void pause(boolean pause) {
				System.out.println("pause " + pause);
				
			}
		});
		fp.setDevice(device);
		fp.setSequence(seq);
		fp.play();
		
		while(true) {
			System.out.println("(P)ause ");
			String op = new BufferedReader(new InputStreamReader(System.in)).readLine();
			if(op.equalsIgnoreCase("p")) {
				fp.setPaused(!fp.isPaused());
			}
		}
	}
}
