package dev.unnm3d.redischat.integrations;

public interface TagResolverIntegration {

    default String resolve(String message){
        return message;
    }

}
