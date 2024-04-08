package net.atlas.combatify.config;

import com.google.common.collect.Maps;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import io.netty.buffer.ByteBuf;
import net.atlas.combatify.networking.NetworkingHandler;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class AtlasConfig {
    public final ResourceLocation name;
	public boolean isDefault;
    private final Map<String, ConfigHolder<?>> valueNameToConfigHolderMap = Maps.newHashMap();
    public static final Map<ResourceLocation, AtlasConfig> configs = Maps.newHashMap();
    final Path configFolderPath;
    File configFile;
    JsonObject configJsonObject;
    List<EnumHolder<?>> enumValues;
    List<StringHolder> stringValues;
    List<BooleanHolder> booleanValues;
    List<IntegerHolder> integerValues;
    List<DoubleHolder> doubleValues;
    public AtlasConfig(ResourceLocation name) {
        enumValues = new ArrayList<>();
        stringValues = new ArrayList<>();
        booleanValues = new ArrayList<>();
        integerValues = new ArrayList<>();
        doubleValues = new ArrayList<>();
        defineConfigHolders();
        this.name = name;
        configFolderPath = Path.of(FabricLoader.getInstance().getConfigDir().getFileName().getFileName() + "/" + name.getNamespace());
        if (!Files.exists(configFolderPath))
            try {
                Files.createDirectory(configFolderPath);
            } catch (IOException e) {
                throw new ReportedException(new CrashReport("Failed to create config directory for config " + name, e));
            }

        load();
        configs.put(name, this);
    }

    public abstract void defineConfigHolders();

	public abstract void resetExtraHolders();

    public abstract <T> void alertChange(ConfigValue<T> tConfigValue, T newValue);

    public static String getString(JsonObject element, String name) {
        return element.get(name).getAsString();
    }

    public static Integer getInt(JsonObject element, String name) {
        return element.get(name).getAsInt();
    }

    public static Double getDouble(JsonObject element, String name) {
        return element.get(name).getAsDouble();
    }
    public static Boolean getBoolean(JsonObject element, String name) {
        return element.get(name).getAsBoolean();
    }
	public void reload() {
		resetExtraHolders();
		load();
	}
    public final void load() {
		isDefault = false;
        configFile = new File(configFolderPath.toAbsolutePath() + "/" + name.getPath() + ".json");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                InputStream inputStream = getDefaultedConfig();
                Files.write(configFile.toPath(), inputStream.readAllBytes());
                inputStream.close();
            } catch (IOException e) {
                throw new ReportedException(new CrashReport("Failed to create config file for config " + name, e));
            }
        }

        try {
            configJsonObject = JsonParser.parseReader(new JsonReader(new FileReader(configFile))).getAsJsonObject();
            for (EnumHolder<?> enumHolder : enumValues)
                if (configJsonObject.has(enumHolder.heldValue.name))
                    enumHolder.setValue(getString(configJsonObject, enumHolder.heldValue.name));
            for (StringHolder stringHolder : stringValues)
                if (configJsonObject.has(stringHolder.heldValue.name))
                    stringHolder.setValue(getString(configJsonObject, stringHolder.heldValue.name));
            for (BooleanHolder booleanHolder : booleanValues)
                if (configJsonObject.has(booleanHolder.heldValue.name))
                    booleanHolder.setValue(getBoolean(configJsonObject, booleanHolder.heldValue.name));
            for (IntegerHolder integerHolder : integerValues)
                if (configJsonObject.has(integerHolder.heldValue.name))
                    integerHolder.setValue(getInt(configJsonObject, integerHolder.heldValue.name));
            for (DoubleHolder doubleHolder : doubleValues)
                if (configJsonObject.has(doubleHolder.heldValue.name))
                    doubleHolder.setValue(getDouble(configJsonObject, doubleHolder.heldValue.name));
            loadExtra(configJsonObject);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    protected abstract void loadExtra(JsonObject jsonObject);
    protected abstract InputStream getDefaultedConfig();
    public AtlasConfig loadFromNetwork(FriendlyByteBuf buf) {
        enumValues.forEach(enumHolder -> enumHolder.readFromBuf(buf));
        stringValues.forEach(stringHolder -> stringHolder.readFromBuf(buf));
        booleanValues.forEach(booleanHolder -> booleanHolder.readFromBuf(buf));
        integerValues.forEach(integerHolder -> integerHolder.readFromBuf(buf));
        doubleValues.forEach(doubleHolder -> doubleHolder.readFromBuf(buf));
        return this;
    }
    public static AtlasConfig staticLoadFromNetwork(FriendlyByteBuf buf) {
        return configs.get(buf.readResourceLocation()).loadFromNetwork(buf);
    }

    public void saveToNetwork(FriendlyByteBuf buf) {
        enumValues.forEach(enumHolder -> enumHolder.writeToBuf(buf));
        stringValues.forEach(stringHolder -> stringHolder.writeToBuf(buf));
        booleanValues.forEach(booleanHolder -> booleanHolder.writeToBuf(buf));
        integerValues.forEach(integerHolder -> integerHolder.writeToBuf(buf));
        doubleValues.forEach(doubleHolder -> doubleHolder.writeToBuf(buf));
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
    public ConfigHolder<?> fromValue(ConfigValue<?> value) {
        return valueNameToConfigHolderMap.get(value.name);
    }
    public final <E extends Enum<E>> EnumHolder<E> createEnum(String name, E defaultVal, Class<E> clazz, E[] values) {
        EnumHolder<E> enumHolder = new EnumHolder<>(new ConfigValue<>(defaultVal, values, false, name, this), clazz);
        enumValues.add(enumHolder);
        return enumHolder;
    }
    public StringHolder createStringRange(String name, String defaultVal, String... values) {
        StringHolder stringHolder = new StringHolder(new ConfigValue<>(defaultVal, values, false, name, this));
        stringValues.add(stringHolder);
        return stringHolder;
    }
	public StringHolder createString(String name, String defaultVal) {
		StringHolder stringHolder = new StringHolder(new ConfigValue<>(defaultVal, null, false, name, this));
		stringValues.add(stringHolder);
		return stringHolder;
	}
    public BooleanHolder createBoolean(String name, boolean defaultVal) {
        BooleanHolder booleanHolder = new BooleanHolder(new ConfigValue<>(defaultVal, new Boolean[]{false, true}, false, name, this));
        booleanValues.add(booleanHolder);
        return booleanHolder;
    }
	public IntegerHolder createIntegerUnbound(String name, Integer defaultVal) {
		IntegerHolder integerHolder = new IntegerHolder(new ConfigValue<>(defaultVal, null, false, name, this));
		integerValues.add(integerHolder);
		return integerHolder;
	}
    public IntegerHolder createInRestrictedValues(String name, Integer defaultVal, Integer... values) {
        IntegerHolder integerHolder = new IntegerHolder(new ConfigValue<>(defaultVal, values, false, name, this));
        integerValues.add(integerHolder);
        return integerHolder;
    }
    public IntegerHolder createInRange(String name, int defaultVal, int min, int max) {
        Integer[] range = new Integer[]{min, max};
        IntegerHolder integerHolder = new IntegerHolder(new ConfigValue<>(defaultVal, range, true, name, this));
        integerValues.add(integerHolder);
        return integerHolder;
    }
	public DoubleHolder createDoubleUnbound(String name, Double defaultVal) {
		DoubleHolder doubleHolder = new DoubleHolder(new ConfigValue<>(defaultVal, null, false, name, this));
		doubleValues.add(doubleHolder);
		return doubleHolder;
	}
    public DoubleHolder createInRestrictedValues(String name, Double defaultVal, Double... values) {
        DoubleHolder doubleHolder = new DoubleHolder(new ConfigValue<>(defaultVal, values, false, name, this));
        doubleValues.add(doubleHolder);
        return doubleHolder;
    }
    public DoubleHolder createInRange(String name, double defaultVal, double min, double max) {
        Double[] range = new Double[]{min, max};
        DoubleHolder doubleHolder = new DoubleHolder(new ConfigValue<>(defaultVal, range, true, name, this));
        doubleValues.add(doubleHolder);
        return doubleHolder;
    }

    public record ConfigValue<T>(T defaultValue, T[] possibleValues, boolean isRange, String name, AtlasConfig owner) {
        public void emitChanged(T newValue) {
            owner.alertChange(this, newValue);
        }
        public boolean isValid(T newValue) {
            return possibleValues == null || Arrays.stream(possibleValues).toList().contains(newValue);
        }
        public void addAssociation(ConfigHolder<T> configHolder) {
            if (owner.valueNameToConfigHolderMap.containsKey(name))
                throw new ReportedException(new CrashReport("Tried to associate a ConfigHolder to a ConfigValue which already has one!", new RuntimeException()));
            owner.valueNameToConfigHolderMap.put(name, configHolder);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConfigValue<?> that)) return false;
            return isRange() == that.isRange() && Objects.equals(defaultValue, that.defaultValue) && Arrays.equals(possibleValues, that.possibleValues) && Objects.equals(name, that.name) && Objects.equals(owner, that.owner);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(defaultValue, isRange(), name, owner);
            result = 31 * result + Arrays.hashCode(possibleValues);
            return result;
        }
    }
    public static abstract class ConfigHolder<T> {
        private T value;
        public final ConfigValue<T> heldValue;
        public final StreamCodec<ByteBuf, T> codec;
        public ConfigHolder(ConfigValue<T> value, StreamCodec<ByteBuf, T> codec) {
            this.value = value.defaultValue;
            heldValue = value;
            this.codec = codec;
            value.addAssociation(this);
        }
        public T get() {
            return value;
        }
        public void writeToBuf(FriendlyByteBuf buf) {
            codec.encode(buf, value);
        }
        public void readFromBuf(FriendlyByteBuf buf) {
            T newValue = codec.decode(buf);
            if (isNotValid(newValue))
                return;
            heldValue.emitChanged(newValue);
            value = newValue;
        }
        public boolean isNotValid(T newValue) {
            return !heldValue.isValid(newValue);
        }
        public void setValue(T newValue) {
            if (isNotValid(newValue))
                return;
            heldValue.emitChanged(newValue);
            value = newValue;
        }
    }
    public static class EnumHolder<E extends Enum<E>> extends ConfigHolder<E> {
        public final Class<E> clazz;
        private EnumHolder(ConfigValue<E> value, Class<E> clazz) {
            super(value, new StreamCodec<>() {
                @Override
                public void encode(ByteBuf object, E object2) {
                    new FriendlyByteBuf(object).writeEnum(object2);
                }

                @Override
                public E decode(ByteBuf object) {
                    return new FriendlyByteBuf(object).readEnum(clazz);
                }
            });
            this.clazz = clazz;

        }
        public void setValue(String name) {
            setValue(Enum.valueOf(clazz, name.toUpperCase(Locale.ROOT)));
        }
    }
    public static class StringHolder extends ConfigHolder<String> {
        private StringHolder(ConfigValue<String> value) {
            super(value, ByteBufCodecs.STRING_UTF8);
        }
    }
    public static class BooleanHolder extends ConfigHolder<Boolean> {
        private BooleanHolder(ConfigValue<Boolean> value) {
            super(value, ByteBufCodecs.BOOL);
        }
    }
    public static class IntegerHolder extends ConfigHolder<Integer> {
        private IntegerHolder(ConfigValue<Integer> value) {
            super(value, ByteBufCodecs.VAR_INT);
        }

        @Override
        public boolean isNotValid(Integer newValue) {
			if (heldValue.possibleValues == null)
				return super.isNotValid(newValue);
            boolean inRange = heldValue.isRange && newValue >= heldValue.possibleValues[0] && newValue <= heldValue.possibleValues[1];
            return super.isNotValid(newValue) && !inRange;
        }
    }
    public static class DoubleHolder extends ConfigHolder<Double> {
        private DoubleHolder(ConfigValue<Double> value) {
            super(value, ByteBufCodecs.DOUBLE);
        }

        @Override
        public boolean isNotValid(Double newValue) {
			if (heldValue.possibleValues == null)
				return super.isNotValid(newValue);
            boolean inRange = heldValue.isRange && newValue >= heldValue.possibleValues[0] && newValue <= heldValue.possibleValues[1];
            return super.isNotValid(newValue) && !inRange;
        }
    }

	public void reloadFromDefault() {
		resetExtraHolders();
		isDefault = true;
		JsonElement configJsonElement = JsonParser.parseReader(new JsonReader(new InputStreamReader(getDefaultedConfig())));

		JsonObject configJsonObject = configJsonElement.getAsJsonObject();
		for (EnumHolder<?> enumHolder : enumValues)
			if (configJsonObject.has(enumHolder.heldValue.name))
				enumHolder.setValue(getString(configJsonObject, enumHolder.heldValue.name));
		for (StringHolder stringHolder : stringValues)
			if (configJsonObject.has(stringHolder.heldValue.name))
				stringHolder.setValue(getString(configJsonObject, stringHolder.heldValue.name));
		for (BooleanHolder booleanHolder : booleanValues)
			if (configJsonObject.has(booleanHolder.heldValue.name))
				booleanHolder.setValue(getBoolean(configJsonObject, booleanHolder.heldValue.name));
		for (IntegerHolder integerHolder : integerValues)
			if (configJsonObject.has(integerHolder.heldValue.name))
				integerHolder.setValue(getInt(configJsonObject, integerHolder.heldValue.name));
		for (DoubleHolder doubleHolder : doubleValues)
			if (configJsonObject.has(doubleHolder.heldValue.name))
				doubleHolder.setValue(getDouble(configJsonObject, doubleHolder.heldValue.name));
		loadExtra(configJsonObject);
	}

	public abstract void handleExtraSync(NetworkingHandler.AtlasConfigPacket packet, LocalPlayer player, PacketSender sender);
}
