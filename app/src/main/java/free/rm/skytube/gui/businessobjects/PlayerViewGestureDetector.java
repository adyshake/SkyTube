/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Gesture detector for a PlayerView.  It will detect gestures that may result in showing comments,
 * video description, change in volume or brightness.
 */
public abstract class PlayerViewGestureDetector implements View.OnTouchListener {

	private GestureDetector             gestureDetector;
	private PlayerViewGestureListener   playerViewGestureListener;


	public PlayerViewGestureDetector(Context context) {
		playerViewGestureListener = new PlayerViewGestureListener();
		gestureDetector = new GestureDetector(context, playerViewGestureListener);
	}


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		gestureDetector.onTouchEvent(event);

		if (event.getAction() == MotionEvent.ACTION_UP) {
			playerViewGestureListener.onSwipeGestureDone();
			onGestureDone();
		}

		return true;
	}


	/**
	 * Called when the user single taps.
	 *
	 * @return  True if the event was consumed; false otherwise.
	 */
	public abstract boolean onSingleTap();


	/**
	 * Called when the user double taps.
	 */
	public abstract void onDoubleTap();


	/**
	 * Called when user wants to view video's description.
	 */
	public abstract void onVideoDescriptionGesture();


	/**
	 * Called every time any gesture is ended.
	 */
	public abstract void onGestureDone();

	/**
	 * User swiped from left to right or from right to left at any place of the view except 20% from the right.
	 */
	public abstract void adjustVideoPosition(double adjustPercent, boolean forwardDirection);


	/**
	 * Returns the PlayerView's Rect instance.
	 *
	 * @return PlayerView's rect.
	 */
	public abstract Rect getPlayerViewRect();


	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Gesture type.
	 */
	private enum SwipeGestureType {
		NONE,
		BRIGHTNESS,
		VOLUME,
		SEEK,
		COMMENTS,
		DESCRIPTION
	}


	/**
	 * Class that listen to events and classifies them accordingly.  Once an event is classified,
	 * it will call the respective (abstract) method.
	 */
	private class PlayerViewGestureListener extends GestureDetector.SimpleOnGestureListener {

		/** The current swipe gesture type being performed by the user (if any). */
		SwipeGestureType currentGestureEvent = SwipeGestureType.NONE;

		private static final int SWIPE_THRESHOLD = 50;


		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return onSingleTap();
		}


		@Override
		public boolean onDoubleTap(MotionEvent e) {
			PlayerViewGestureDetector.this.onDoubleTap();
			return false;
		}


		@Override
		public boolean onScroll(MotionEvent startEvent, MotionEvent endEvent, float distanceX, float distanceY) {
			// detect swipe event type
			currentGestureEvent = getSwipeGestureType(startEvent, endEvent);

			if (currentGestureEvent != SwipeGestureType.NONE) {
				double  yDistance = endEvent.getY() - startEvent.getY();
				double  xDistance = endEvent.getX() - startEvent.getX();
				Rect    playerViewRect = getPlayerViewRect();

				if (currentGestureEvent == SwipeGestureType.DESCRIPTION) {
					onVideoDescriptionGesture();
				}
				else if (currentGestureEvent == SwipeGestureType.SEEK) {
					double percent = xDistance / getPlayerViewRect().width();
					adjustVideoPosition(percent, distanceX < 0);
				}
			}

			return false;   // event not consumed -- the event might need to be consumed by the Video Player
		}


		/**
		 * To be called when the user is done swiping.
		 */
		void onSwipeGestureDone() {
			currentGestureEvent = SwipeGestureType.NONE;
		}


		/**
		 * Detect swipe gesture type.
		 *
		 * @param startEvent    The start event.
		 * @param currentEvent  The current event.
		 *
		 * @return The detected {@link SwipeGestureType}.
		 */
		private SwipeGestureType getSwipeGestureType(MotionEvent startEvent, MotionEvent currentEvent) {
			if (currentGestureEvent != SwipeGestureType.NONE) {
				return currentGestureEvent;
			}

			final float startX = startEvent.getX();
			final float startY = startEvent.getY();
			final float currentX = currentEvent.getX();
			final float currentY = currentEvent.getY();
			final float diffY = startY - currentY;
			final float diffX = startX - currentX;
			final Rect  playerViewRect = getPlayerViewRect();

			if (Math.abs(currentX - startX) >= SWIPE_THRESHOLD && Math.abs(currentX - startX) > Math.abs(currentY - startY)) {
				return SwipeGestureType.SEEK;
			} else if (Math.abs(currentY - startY) >= SWIPE_THRESHOLD) {
				if (getDescriptionRect(playerViewRect).contains((int) startX, (int) startY) && diffY > 0) {
					return SwipeGestureType.DESCRIPTION;
				}
			}

			return SwipeGestureType.NONE;
		}

		/**
		 * Here we choose a rect for swipe which then will be used to open the description view.
		 */
		private Rect getDescriptionRect(final Rect playerViewRect) {
			return new Rect(0,0, playerViewRect.right, (int) (playerViewRect.bottom - (playerViewRect.bottom * 0.2)));
		}

	}

}
