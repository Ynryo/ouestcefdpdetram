package fr.ynryo.ouestcefdpdetram;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FollowManager {
    private final MainActivity context;
    private boolean isFollowing;
    private String followedMarkerId;

    public FollowManager(MainActivity context) {
        this.context = context;
        this.isFollowing = false;
    }

    public void enableFollow() {
        if (!isFollowing) {
            isFollowing = true;
        }
        if (followedMarkerId != null) {
            centerOnFollowed();
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
    
    public void disableFollow() {
        if (isFollowing) {
            isFollowing = false;
            followedMarkerId = null;
        }
    }
    
    public String getFollowedMarkerId() {
        return followedMarkerId;
    }

    public void setFollowButton(FloatingActionButton followButton) {
        if (followButton != null) {
            followButton.setOnClickListener(view -> enableFollow());
        }
    }
}
