# SVG to Vector Drawable

The `res/.../ic_launcher.png` files were generated using the other files in Android Studio.

`res/drawable/forkyz_icon.xml` and `forkyz_icon_monotone.xml` are generated from `forkyz_icon.svg` in this folder. I used [this converter][svg-to-android]. The output wasn't quite right: it seems to remove the 18dp border, and the gradient is not supported. Fix by

* Changing android:width and android:height to 108dp.
* Changing android:viewportWidth and android:veiwportHeight to 67.5.
* Changing the second android:translateX and android:translateY to 26.5 from 7.49.
* Replace the url fillColor with #ffffff.

To create the monochrome version, remove all strokeColor and fillColor that are not #000000.

[svg-to-android]: http://inloop.github.io/svg2android/


