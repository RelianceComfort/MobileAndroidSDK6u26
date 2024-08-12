package com.metrix.architecture.superclasses;

/**
 * This interface should be applied to any activity which 
 * wants to implement implicit saves as part of swipe functionality.
 * 
 * @since 5.6
 */
public interface MetrixImplicitSaveSwipeActivity {
	boolean implicitSwipeSave();
}
