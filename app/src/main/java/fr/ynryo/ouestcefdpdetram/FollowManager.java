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
        if (isFollowing) {
            disableFollow();
        } else {
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

    public void setFollowButton(FloatingActionButton followButton, String followedMarkerId) {
        this.followButton = followButton;
        if (this.followButton == null) return;
        if (isFollowing) this.followButton.setAlpha(0.6f);
        this.followButton.setOnClickListener(view -> toggleFollow(followedMarkerId));
    }
}
