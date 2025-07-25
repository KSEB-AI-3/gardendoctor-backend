package com.project.farming.global.image.entity;

public class DefaultImages {

    // S3에 저장된 파일명(s3Key)
    public static final String DEFAULT_PLANT_IMAGE = "default_plant.png";
    public static final String DEFAULT_FARM_IMAGE = "default_farm.jpg";

    public static boolean isDefaultImage(String imageName) {
        return DEFAULT_PLANT_IMAGE.equals(imageName)
                || DEFAULT_FARM_IMAGE.equals(imageName);
    }
}
