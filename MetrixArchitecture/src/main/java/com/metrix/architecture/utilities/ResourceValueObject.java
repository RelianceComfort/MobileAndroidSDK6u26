package com.metrix.architecture.utilities;

/**
 * Created by hesplk on 11/4/2016.
 */
public class ResourceValueObject {
    public Integer resourceId;
    public String messageId;
    public boolean isHint;


    public ResourceValueObject(Integer resourceId, String messageId, boolean isHint) {
        this.messageId = messageId;
        this.resourceId = resourceId;
        this.isHint = isHint;
    }

    public ResourceValueObject(Integer resourceId, String messageId) {
        this.messageId = messageId;
        this.resourceId = resourceId;
    }
}
