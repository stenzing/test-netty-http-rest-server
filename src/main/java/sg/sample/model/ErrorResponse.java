package sg.sample.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@Getter
@JsonSerialize(using = ErrorResponse.CustomSerializer.class)
public class ErrorResponse implements APIResponse {
    private final String txId;
    private final Exception exception;

    @Override
    public String toString() {
        return exception.getMessage();
    }

    public static class CustomSerializer extends JsonSerializer<ErrorResponse> {

        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void serialize(ErrorResponse errorResponse, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            if (errorResponse.getTxId() != null) {
                generator.writeFieldName("txId");
                generator.writeString(errorResponse.getTxId());
            }
            generator.writeFieldName("error");
            generator.writeString(errorResponse.getException().getMessage());
            generator.writeEndObject();
        }
    }
}
