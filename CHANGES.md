# 1.0.0-SNAPSHOT-${buildNumber}

 * Added layout for Razer Base Station Chroma V2 .
 * Updated to Java 16. This lets us use dbus-java 4.0.0-SNAPSHOT.
 * Updated to dbus-java 4.0.0-SNAPSHOT. This lets us drop a lot of dependencies.
 * Added an uninstaller (run bin/uninstaller)

# 1.0.0-SNAPSHOT-173

 * Fails to start if `jscal` or `jstest-gtk` are not installed.
   
# 1.0.0-SNAPSHOT-172

 * OpenJ9 wasn't actually activated (due to problem with jimpulse), but it now is.

# 1.0.0-SNAPSHOT-151

 * Switch to OpenJ9 for lower memory usage and faster startup time. 
 
 * Updated Dorkbox SystemTray to latest.
 
 * Added splash screen.

# 1.0.0-SNAPSHOT-148

 * Testing repository location accidentally baked into build. You will need to re-install or use 

   ```
   bin/snake --remote-manifest http://www.bithatch.co.uk/repositories/snake/snapshot/manifest.xml
   ```

   .. or if you are using the 'no-runtime' package ..
      
   ```
   bin/snake --remote-manifest http://www.bithatch.co.uk/repositories/snake-noruntime/snapshot/manifest.xml
   ```
   
# 1.0.0-SNAPSHOT-145
 
 * Audio lighting effects. Requires `libpulse` and `libfftw-3` to be installed. 

 * A 3rd  Macro/remapping implementation, this time based on my own MacroLib, a port of Gnome15's
   macro system to Java. Automatic profile selection is supported, but you will need `bamfdaemon` 
   installed (there is a fallback planned that uses libwnck3 but this is currently faulty).
   
 * Support hot-plugging devices.
 
 * Failed to start when "Start on login" selected.
 
 * Fixed some memory leaks.
 
 * Issues with switching between effects. Matrix effects especially could not be stacked.
 
 * Some UI improvements. "Controls" stand out better against the background. More consistent
   spacing. 
   
 * Incremental custom effects improvements. Each cell in a keyframe can now take either it's
   Hue, Saturation or Brightness from a static colour, the audio level or a random value. 
   This opens up a few more possibilities for custom effects. More to come in future releases.
   
 * New colour picker (http://llbit.se/?p=3331)[LuxColorPicker].
 
 * Due to font conflict when FontAwesome is installed locally or system-wide, icon usage 
   completely reworked.
 
 * Lots of internal stuff. Better modularisation of a number of application and 3rd party libraries
   (ongoing, will ultimately lead to a lighter install). Much smaller command line used to
   launch the app and update (easier to read in `ps` command etc).

# 1.0.0-SNAPSHOT-24

 * Better support for Tartarus V2. 
 
 * Partial implementation of new macro system (https://github.com/openrazer/openrazer/pull/1124).
   More to come.
   

# 1.0.0-SNAPSHOT-20

 * Layout Editor. Can be used to create graphical layouts of Razer devices for a more 
   interesting user interface. These layouts may then be exported and shared with others
   (including to the Snake project for inclusions). The intention is over time, all devices
   have their own layout. The layouts are then used in all other aspects of Snake, such
   as the main UI, the custom effects editor and the macro user interface.
   
 * New main user interface making use of layouts (if your device has one). 
      
 * You can now create custom effects on devices that have the Matrix capability. Animations
   are based around the concept of Key Frames. All lights in the matrix interpolate between
   the current key frame and the next (using a configurable algorithm). The effects can
   be exported the be shared with others as an add-on.
   
 * Smoother UI effects.
 
 * Lots of internal refactoring. 

# 1.0.0-SNAPSHOT-1

 * Saner version numbers
 * New original application and tray icon
 * Cope with OpenRazer DBUS API version differences with getDeviceImage.
 * When opening from the tray, navigation could get messed up.

# 1.0.0-SNAPSHOT-0

Initial release of *Snake*.
