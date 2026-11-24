package com.villagerking.voice;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Path;
import java.util.function.Consumer;

public class VoiceListener {
    private final Model model;
    private volatile boolean recording = false;

    public VoiceListener(Path modelDir) throws Exception {
        this.model = new Model(modelDir.toString());
    }

    public void start(Consumer<String> onText) {
        if (recording) return;
        recording = true;
        Thread worker = new Thread(() -> {
            AudioFormat fmt = new AudioFormat(16000f, 16, 1, true, false);
            try (TargetDataLine mic = AudioSystem.getTargetDataLine(fmt);
                 Recognizer rec = new Recognizer(model, 16000f)) {
                mic.open(fmt);
                mic.start();
                byte[] buf = new byte[4096];
                while (recording) {
                    int n = mic.read(buf, 0, buf.length);
                    if (n > 0) rec.acceptWaveForm(buf, n);
                }
                String json = rec.getFinalResult();
                String text = json.replaceAll("(?s).*\"text\"\\s*:\\s*\"(.*?)\".*", "$1").trim();
                if (!text.isEmpty()) onText.accept(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "villager-voice");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() { recording = false; }
}
