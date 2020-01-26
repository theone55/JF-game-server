package com.justfors;

public class GameObject {
    private String owner;
    private String objectType;
    private Double x;
    private Double y;
    private Long countOfCalls = 0L;
    private String direction;

    public GameObject(String owner, String objectType, Double x, Double y, String direction) {
        this.owner = owner;
        this.objectType = objectType;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    public String getOwner() {
        return owner;
    }

    public String getObjectType() {
        return objectType;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Long getCountOfCalls() {
        return countOfCalls;
    }

    public String getDirection() {
        return direction;
    }

    public void incrimentCountOfCall(){
        this.countOfCalls++;
    }
}
