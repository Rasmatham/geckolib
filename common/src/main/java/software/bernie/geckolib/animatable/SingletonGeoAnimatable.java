package software.bernie.geckolib.animatable;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.GeckoLibServices;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * The {@link GeoAnimatable} interface specific to singleton objects
 * <p>
 * This primarily applies to armor and items
 *
 * @see <a href="https://github.com/bernie-g/geckolib/wiki/Item-Animations">GeckoLib Wiki - Item Animations</a>
 */
public interface SingletonGeoAnimatable extends GeoAnimatable {
    /**
     * Register this as a synched {@code GeoAnimatable} instance with GeckoLib's networking functions
     * <p>
     * This should be called inside the constructor of your object.
     */
    static void registerSyncedAnimatable(GeoAnimatable animatable) {
        GeckoLibUtil.registerSyncedAnimatable(animatable);
    }

    /**
     * Get server-synced animation data via its relevant {@link SerializableDataTicket}
     * <p>
     * Should only be used on the <u>client-side</u>
     * <p>
     * <b><u>DO NOT OVERRIDE</u></b>
     *
     * @param instanceId The animatable's instance id
     * @param dataTicket The data ticket for the data to retrieve
     * @return The synced data, or null if no data of that type has been synced
     */
    @ApiStatus.NonExtendable
    @Nullable
    default <D> D getAnimData(long instanceId, SerializableDataTicket<D> dataTicket) {
        return getAnimatableInstanceCache().getManagerForId(instanceId).getData(dataTicket);
    }

    /**
     * Saves an arbitrary piece of syncable data to this animatable's {@link AnimatableManager}
     * <p>
     * <b><u>DO NOT OVERRIDE</u></b>
     *
     * @param relatedEntity An entity related to the state of the data for syncing (E.G. The player holding the item)
     * @param instanceId    The unique id that identifies the specific animatable instance
     * @param dataTicket    The DataTicket to sync the data for
     * @param data          The data to sync
     */
    @ApiStatus.NonExtendable
    default <D> void setAnimData(Entity relatedEntity, long instanceId, SerializableDataTicket<D> dataTicket, D data) {
        if (relatedEntity.level().isClientSide()) {
            getAnimatableInstanceCache().getManagerForId(instanceId).setData(dataTicket, data);
        }
        else {
            syncAnimData(instanceId, dataTicket, data, relatedEntity);
        }
    }

    /**
     * Syncs an arbitrary piece of data to all players targeted by the packetTarget
     * <p>
     * This method should only be called on the <u>server side</u>
     * <p>
     * <b><u>DO NOT OVERRIDE</u></b>
     *
     * @param instanceId The unique id that identifies the specific animatable instance
     * @param dataTicket The DataTicket to sync the data for
     * @param data       The data to sync
     */
    @ApiStatus.NonExtendable
    default <D> void syncAnimData(long instanceId, SerializableDataTicket<D> dataTicket, D data, Entity entityToTrack) {
        GeckoLibServices.NETWORK.syncSingletonAnimData(this, instanceId, dataTicket, data, entityToTrack);
    }

    /**
     * Trigger a client-side animation for this GeoAnimatable for the given controller name and animation name
     * <p>
     * This can be fired from either the client or the server, but optimally you would call it from the server
     * <p>
     * <b><u>DO NOT OVERRIDE</u></b>
     *
     * @param relatedEntity  An entity related to the animatable to trigger the animation for (E.G. The player holding the item)
     * @param instanceId     The unique id that identifies the specific animatable instance
     * @param controllerName The name of the controller the animation belongs to, or null to do an inefficient lazy search
     * @param animName       The name of animation to trigger. This needs to have been registered with the controller via {@link AnimationController#triggerableAnim AnimationController.triggerableAnim}
     */
    @ApiStatus.NonExtendable
    default <D> void triggerAnim(Entity relatedEntity, long instanceId, @Nullable String controllerName, String animName) {
        if (relatedEntity.level().isClientSide()) {
            if (controllerName != null) {
                getAnimatableInstanceCache().getManagerForId(instanceId).tryTriggerAnimation(controllerName, animName);
            }
            else {
                getAnimatableInstanceCache().getManagerForId(instanceId).tryTriggerAnimation(animName);
            }
        }
        else {
            GeckoLibServices.NETWORK.triggerSingletonAnim(this, relatedEntity, instanceId, controllerName, animName);
        }
    }

    /**
     * Stop a previously triggered animation for this GeoAnimatable for the given controller name and animation name
     * <p>
     * This can be fired from either the client or the server, but optimally you would call it from the server
     * <p>
     * <b><u>DO NOT OVERRIDE</u></b>
     *
     * @param relatedEntity An entity related to the animatable to trigger the animation for (E.G. The player holding the item)
     * @param instanceId The unique id that identifies the specific animatable instance
     * @param controllerName The name of the controller the animation belongs to, or null to do an inefficient lazy search
     * @param animName The name of the triggered animation to stop, or null to stop any currently playing triggered animation
     */
    @ApiStatus.NonExtendable
    default void stopTriggeredAnim(Entity relatedEntity, long instanceId, @Nullable String controllerName, @Nullable String animName) {
        if (relatedEntity.level().isClientSide()) {
            AnimatableManager<GeoAnimatable> animatableManager = getAnimatableInstanceCache().getManagerForId(instanceId);

            if (animatableManager == null)
                return;

            if (controllerName != null) {
                animatableManager.stopTriggeredAnimation(controllerName, animName);
            }
            else {
                animatableManager.stopTriggeredAnimation(animName);
            }
        }
        else {
            GeckoLibServices.NETWORK.stopTriggeredSingletonAnim(getClass(), relatedEntity, instanceId, controllerName, animName);
        }
    }

    /**
     * Override the default handling for instantiating an AnimatableInstanceCache for this animatable
     * <p>
     * Don't override this unless you know what you're doing
     */
    @Override
    default @Nullable AnimatableInstanceCache animatableCacheOverride() {
        return new SingletonAnimatableInstanceCache(this);
    }

    /**
     * Create your GeoRenderProvider reference here
     * <p>
     * <b><u>MUST provide an anonymous class</u></b>
     * <p>
     * Example Code:
     * <pre>{@code
     * @Override
     * public void createRenderer(Consumer<GeoRenderProvider> consumer) {
     * 	consumer.accept(new GeoRenderProvider() {
     * 		private final BlockEntityWithoutLevelRenderer itemRenderer;
     *
     *        @Override
     *        @Nullable
     *        BlockEntityWithoutLevelRenderer getItemRenderer(GeoArmor armor) {
     *          if (this.itemRenderer == null)
     *              this.itemRenderer = new MyItemRenderer();
     *
     * 			return this.itemRenderer;
     *        }
     *    }
     * }
     * }</pre>
     *
     * @param consumer Consumer of your new GeoRenderProvider instance
     */
    default void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {}

    /**
     * Get the cached {@link GeoRenderProvider} from your {@link AnimatableInstanceCache}
     */
    default Object getRenderProvider() {
        return getAnimatableInstanceCache().getRenderProvider();
    }
}
