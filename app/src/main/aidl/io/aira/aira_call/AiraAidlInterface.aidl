package io.aira.aira_call;

interface AiraAidlInterface {
    /**
    * Returns whether the microphone is currently enabled
    */
    boolean isMicrophoneEnabled();
    /**
    * Enables or disables the microphone for an on going call
    */
    void setMicrophoneEnabled(boolean enabled);

    /**
    * Returns whether the camera is currently enabled
    */
    boolean isCameraEnabled();
    /**
    * Enables or disables the camera for an ongoing call
    */
    void setCameraEnabled(boolean enabled);

    /**
    * Returns whether the user is currently in an active call
    */
    boolean isInCall();
    /**
    * Ends the current call
    */
    void endCall();

    /**
    * Returns the logged in user email or phone. If not logged in, returns null
    */
    String getLoggedInUser();

    /**
    * Logs out the user
    */
    void logout();
}