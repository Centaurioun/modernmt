package eu.modernmt.cluster.kafka;

import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;

/** Created by davide on 30/08/16. */
public class KafkaPacketDeserializer implements Deserializer<KafkaPacket> {

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // No options
  }

  @Override
  public KafkaPacket deserialize(String topic, byte[] data) {
    if (data == null) return null;

    return KafkaPacket.fromBytes(data);
  }

  @Override
  public void close() {
    // Nothing to do
  }
}
