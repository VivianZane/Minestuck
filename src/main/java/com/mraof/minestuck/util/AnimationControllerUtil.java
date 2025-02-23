package com.mraof.minestuck.util;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationController;

/**
 * Collection of helper functions for common-side animations.
 */
public final class AnimationControllerUtil
{
	/**
	 * Helper to create a new animation controller with custom animation speed
	 *
	 * @param name      name of this controller
	 * @param speed     animation speed - default speed is 1
	 * @param predicate the animation predicate
	 * @return a configured animation controller with speed
	 */
	public static <T extends GeoAnimatable> AnimationController<T> createAnimation(T entity, String name, double speed, AnimationController.AnimationStateHandler<T> predicate)
	{
		AnimationController<T> controller = new AnimationController<>(entity, name, 0, predicate);
		controller.setAnimationSpeed(speed);
		return controller;
	}
}
