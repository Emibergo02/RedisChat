package dev.unnm3d.redischat.chat.objects;

public interface DataSerializable {

    String serialize();

    static <T extends DataSerializable> T deserialize(String serialized) {
        throw new UnsupportedOperationException("Deserialization not implemented");
    }
}
