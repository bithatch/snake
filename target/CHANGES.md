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
