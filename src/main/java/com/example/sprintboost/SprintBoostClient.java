package com.example.sprintboost;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

public class SprintBoostClient implements ClientModInitializer {
    private static final float MAX_BOOST = 5.0f;        // 上限：原版奔跑速度的 500%
    private static final float BOOST_STEP = 0.1f;       // 每次双击加速 +10%
    private static final float DECAY_STEP = 0.1f;       // 每次自动减速 -10%
    private static final long DOUBLE_CLICK_MS = 500L;   // 0.5 秒内连按两次视为双击
    private static final long BOOST_COOLDOWN_MS = 100L;  // 双击加速后需等 0.1 秒才能再次加速
    private static final long DECAY_DELAY_MS = 1000L;   // 1 秒内没有按下 Ctrl 开始自动减速

    private float boostLevel = 1.0f;
    private long lastClickTime = 0L;        // 上一次按下的时刻（双击窗口检测）
    private long lastPressTime = 0L;        // 最近一次按下的时刻（衰减计时）
    private long lastBoostTime = 0L;        // 最近一次双击加速生效的时刻（冷却计时）
    private long lastDecayTime = 0L;        // 最近一次自动减速的时刻
    private float lastSent = -1f;
    private boolean wasCtrlDown = false;

    @Override
    public void onInitializeClient() {
        // 不注册任何 KeyBinding，避免与原版疾跑键（默认左 Ctrl）冲突，
        // 直接读取物理按键状态，原版疾跑功能完全保留。
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) return;

        // GUI（聊天框、容器界面等）打开时不处理按键
        if (client.currentScreen != null) {
            wasCtrlDown = InputUtil.isKeyPressed(
                    client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL);
            return;
        }

        boolean ctrlDown = InputUtil.isKeyPressed(
                client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL);

        if (player.isSprinting()) {
            long now = System.currentTimeMillis();

            // 按下瞬间（按住 Ctrl 只算一次单击，不保持速度）：
            // 0.5 秒内的第二次按下 = 双击；且距上次双击加速已满 1 秒才生效，
            // 冷却期内的双击当作单击处理（不加速，但仍刷新按下时间以阻止衰减）
            if (ctrlDown && !wasCtrlDown) {
                boolean isDoubleClick = (now - lastClickTime <= DOUBLE_CLICK_MS);
                if (isDoubleClick && (now - lastBoostTime >= BOOST_COOLDOWN_MS)) {
                    boostLevel = Math.min(MAX_BOOST, boostLevel + BOOST_STEP);
                    lastBoostTime = now;
                }
                lastClickTime = now;
                lastPressTime = now;
            }

            // 自动减速：超过 1 秒没有按下 Ctrl（按住、松开都不算"按下"），
            // 每满 1 秒速度减少原版奔跑速度的 10%
            if (now - lastPressTime > DECAY_DELAY_MS) {
                long decayRef = Math.max(lastPressTime, lastDecayTime);
                if (now - decayRef >= DECAY_DELAY_MS) {
                    boostLevel = Math.max(1.0f, boostLevel - DECAY_STEP);
                    lastDecayTime = now;
                }
            }
        } else {
            // 停止冲刺：立即重置到原速
            boostLevel = 1.0f;
        }
        wasCtrlDown = ctrlDown;

        if (boostLevel != lastSent) {
            lastSent = boostLevel;
            ClientPlayNetworking.send(new BoostPayload(boostLevel));
        }
    }
}