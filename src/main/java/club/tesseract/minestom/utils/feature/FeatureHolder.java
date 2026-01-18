package club.tesseract.minestom.utils.feature;

public interface FeatureHolder {

    FeatureContainer getFeatureContainer();


    default void onEnable(){
        getFeatureContainer().onEnable();
    }

    default void onDisable(){
        getFeatureContainer().onDisable();
    }

}
