# Authentication Screen Background Image

This directory should contain the high-resolution background image used for the authentication screens.

## Required Image

Please add the following image to the drawable-xxxhdpi directory:

- **Filename:** food_background.jpg
- **Description:** A high-quality food or cooking-related background image
- **Recommended resolution:** At least 1920x1080 pixels (or higher for better quality on high-resolution screens)

## Image Guidelines

For the best visual experience:
- Choose a dark-tinted or muted food image that won't interfere with the white text
- Select images with good composition (ingredients, dishes, or cooking scenes work well)
- Ensure the image has enough contrast after the gradient overlay is applied
- Avoid images with text or distracting elements in the center

## Alternative

If you don't have a suitable image, you can revert to using the original gradient_background by updating the image source in the layout files:
- activity_login.xml
- activity_signup.xml
- activity_forgot_password.xml

Change:
```xml
android:src="@drawable/food_background"
```

To:
```xml
android:src="@drawable/gradient_background"
``` 