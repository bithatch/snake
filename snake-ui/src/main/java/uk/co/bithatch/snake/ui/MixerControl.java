package uk.co.bithatch.snake.ui;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.Line.Info;
import javax.sound.sampled.Mixer;

public class MixerControl {

	public static void main(String[] args) {
		for(Mixer.Info info : AudioSystem.getMixerInfo()) {
			System.out.println(String.format("Name: %s, Description: %s, Vendor: %s, Version: %s ", info.getName(), info.getDescription(), info.getVendor(), info.getVersion()));
			Mixer mixer = AudioSystem.getMixer(info);
			System.out.println("Controls: ");
			for(Control control : mixer.getControls()) {
				System.out.println("   " + control);
			}
			System.out.println("Source Line Info: ");
			for(Info sourceInfo : mixer.getSourceLineInfo()) {
				System.out.println("   " + sourceInfo);
			}
			System.out.println("Target Line Info: ");
			for(Info targetInfo : mixer.getTargetLineInfo()) {
				System.out.println("   " + targetInfo);
			}
			System.out.println("Target Lines: ");
			for(Line targetLine : mixer.getTargetLines()) {
				System.out.println("   " + targetLine);
			}
		}
	}
}
