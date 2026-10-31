package melhorias.mixin;

import melhorias.MelhoriasConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Controle suave do barco, aplicado somente na água. Em gelo, terra e ar o código original continua valendo
 * (mantém a compatibilidade com barcos em gelo, etc). O atrito da água do jogo continua atuando normalmente.
 */
@Mixin(Boat.class)
public abstract class BoatMixin extends Entity {
    @Shadow private boolean inputLeft;
    @Shadow private boolean inputRight;
    @Shadow private boolean inputUp;
    @Shadow private boolean inputDown;
    @Shadow private float deltaRotation;
    @Shadow private Boat.Status status;
    @Shadow public abstract void setPaddleState(boolean left, boolean right);

    public BoatMixin(EntityType<?> type, Level level) { super(type, level); }

    @Inject(method = "controlBoat", at = @At("HEAD"), cancellable = true)
    private void melhorias$smoothControl(CallbackInfo ci) {
        MelhoriasConfig.Data c = MelhoriasConfig.get;
        if (!c.boatEnabled || !this.isVehicle()) return;
        Boat.Status s = this.status;
        if (s != Boat.Status.IN_WATER && s != Boat.Status.UNDER_WATER && s != Boat.Status.UNDER_FLOWING_WATER) return;

        double max = c.boatMaxSpeed / 20.0;
        double rad = this.getYRot() * (Math.PI / 180.0);
        double fx = Math.sin(-rad), fz = Math.cos(rad);
        Vec3 v = this.getDeltaMovement();
        double hx = v.x, hz = v.z;
        double ratio = Math.min(1.0, Math.hypot(hx, hz) / max);

        // Direção: resposta imediata, mais estável em alta velocidade, volta ao centro ao soltar.
        int dir = (this.inputRight ? 1 : 0) - (this.inputLeft ? 1 : 0);
        float steer = (float) c.boatSteeringSensitivity;
        float rot = this.deltaRotation;
        if (dir != 0) rot += dir * 1.25F * steer * (float) (1.0 - 0.35 * ratio);
        else rot *= 0.8F;
        float lim = 11.0F * steer;
        this.deltaRotation = Math.max(-lim, Math.min(lim, rot));
        this.setYRot(this.getYRot() + this.deltaRotation);
        // a direção mudou: recalcula o vetor frontal
        rad = this.getYRot() * (Math.PI / 180.0);
        fx = Math.sin(-rad);
        fz = Math.cos(rad);

        // Aceleração: arranque mais firme que afina perto da velocidade máxima.
        double a = 0;
        if (this.inputUp) a += 0.04 * c.boatAcceleration * (1.0 + 0.6 * (1.0 - ratio));
        if (this.inputDown) {
            double along = hx * fx + hz * fz;
            if (along > 0.02) {                                   // freio
                double k = 1.0 - Math.min(0.5, 0.08 * c.boatDeceleration);
                hx *= k; hz *= k;
            } else {
                a -= 0.015 * c.boatAcceleration;                  // ré lenta
            }
        }
        if (dir != 0 && !this.inputUp && !this.inputDown) a += 0.005 * c.boatAcceleration;
        hx += fx * a;
        hz += fz * a;

        // Desaceleração ao soltar o acelerador (1.0 = original; menor = desliza mais).
        if (!this.inputUp && !this.inputDown) {
            double k = Math.max(0.9, 1.0 + (1.0 - c.boatDeceleration) * 0.04);
            hx *= k; hz *= k;
        }

        // Derrapagem: alinha gradualmente a velocidade com a proa (0 = aderência total, 1 = original).
        double speed = Math.hypot(hx, hz);
        if (speed > 1.0E-3) {
            double sign = (hx * fx + hz * fz) >= 0 ? 1.0 : -1.0;
            double mix = (1.0 - c.boatDrift) * 0.3;
            hx += (fx * speed * sign - hx) * mix;
            hz += (fz * speed * sign - hz) * mix;
        }

        // Velocidade máxima (somente na água).
        double sp = Math.hypot(hx, hz);
        if (sp > max) { hx *= max / sp; hz *= max / sp; }

        this.setDeltaMovement(hx, v.y, hz);
        this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp,
                this.inputLeft && !this.inputRight || this.inputUp);
        ci.cancel();
    }
}
