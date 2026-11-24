VILLAGER KING MOD  (Fabric, Minecraft 26.3)

WHAT IT DOES
- /king follow | stay | guard | wood | mine | farm | work | castle | house | bring diamond|netherite|gold|iron
- Hold V and speak the same words; the mod types the /king command for you.
- With no order, villagers work by themselves (farmer, woodcutter, miner, guard, builder),
  store items in chests within 30 blocks of you, build houses, and the village grows to 150.

BUILD STEPS
1. Install a JDK (the version your Minecraft 26.3 needs, probably Java 25).
2. Go to https://fabricmc.net/develop , choose Minecraft 26.3, tick "Split client and common sources",
   download the template and unzip it. (Or use your existing mod project.)
3. Copy the "src" folder from this zip into that project, merging/replacing files.
   In fabric.mod.json and the mixins json, make sure the ids match (villagerking).
4. Follow ADD_TO_BUILD_GRADLE.txt.
5. Run:  gradlew build        (Windows: gradlew.bat build)
6. Your mod is build/libs/<name>-1.0.0.jar. Put it in .minecraft/mods together with Fabric API.

VOICE MODEL
Download "vosk-model-small-en-us" from https://alphacephei.com/vosk/models , unzip it, and rename the
folder to "model" so you have:   .minecraft/villager_king/model/...

IF THERE ARE RED ERRORS
This code was written without being able to compile it, and 26.3 may rename a few things
(for example the KeyMapping category, king.level(), or the Mixin method name customServerAiStep).
Send me the exact error lines and I will fix them.
