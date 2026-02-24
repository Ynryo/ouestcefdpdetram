package fr.ynryo.ouestcefdpdetram;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FollowManager {
    private final MainActivity context;
    private boolean isFollowing;
    private String followedMarkerId;
    private FloatingActionButton followButton;

    public FollowManager(MainActivity context) {
        this.context = context;
        this.isFollowing = false;
    }

    public void toggleFollow(String followedMarkerId) {
        if (followedMarkerId == null) return;
        if (isFollowing(followedMarkerId)) {
            disableFollow(false);
        } else {
            disableFollow(true);
            enableFollow(followedMarkerId);
        }
    }

    public void enableFollow(String followedMarkerId) {
        if (!isFollowing && followedMarkerId != null) {
            isFollowing = true;
            this.followedMarkerId = followedMarkerId;
            centerOnFollowed();
            followButton.setAlpha(0.6f);
        }
    }

    public void disableFollow(boolean isGesture) {
        if (isFollowing) {
            isFollowing = false;
            if (followButton != null) {
                followButton.setAlpha(1f);
            }

            if (!isGesture && context.getMap() != null) {
                context.getMap().animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(context.getMap().getCameraPosition().target)
                                .bearing(0)
                                .tilt(0)
                                .zoom(13f)
                                .build()
                ));
            }

            followedMarkerId = null;
        }
    }

    public void setFollowedMarkerId(String markerId) {
        this.followedMarkerId = markerId;
    }

    public boolean isFollowing(String markerId) {
        return isFollowing && followedMarkerId != null && followedMarkerId.equals(markerId);
    }

    public void centerOnFollowed() {
        if (followedMarkerId == null || context.getMap() == null) return;
        context.centerOnMarker(followedMarkerId);
    }
    
    public String getFollowedMarkerId() {
        return followedMarkerId;
    }

    public void setFollowButton(FloatingActionButton followButton, String followedMarkerId) {
        this.followButton = followButton;
        if (this.followButton == null) return;
        if (isFollowing(followedMarkerId)) this.followButton.setAlpha(0.6f);
        this.followButton.setOnClickListener(view -> toggleFollow(followedMarkerId));
    }
}
