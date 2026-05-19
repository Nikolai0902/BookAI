package io.book.ai.llm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body sent to the Anthropic Messages API.
 *
 * @param model          the model identifier (e.g. {@code claude-sonnet-4-6})
 * @param max_tokens     maximum number of tokens to generate in the response
 * @param system         optional system prompt that sets the assistant's behavior
 * @param stop_sequences optional list of strings that stop generation when encountered
 * @param temperature    optional sampling temperature
 * @param messages       the conversation turns to send to the model
 * @param tools          optional list of tool definitions; omitted from JSON when null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicRequest(
        String model,
        int max_tokens,
        String system,
        List<String> stop_sequences,
        Double temperature,
        List<Message> messages,
        List<ToolDefinition> tools
) {

    /**
     * Вспомогательный конструктор для запросов без инструментов (большинство вызывающих).
     */
    public AnthropicRequest(String model, int max_tokens, String system,
                            List<String> stop_sequences, Double temperature,
                            List<Message> messages) {
        this(model, max_tokens, system, stop_sequences, temperature, messages, null);
    }

    /**
     * Единица диалога.
     *
     * @param role    роль — {@code "user"} или {@code "assistant"}
     * @param content текст (String) или список контент-блоков (List)
     */
    public record Message(String role, Object content) {}

    /**
     * Определение инструмента для Anthropic API (tool use).
     *
     * @param name         уникальное имя инструмента
     * @param description  описание для модели — что делает инструмент
     * @param input_schema JSON Schema входных параметров инструмента
     */
    public record ToolDefinition(String name, String description, Object input_schema) {}

    /**
     * Контент-блок с результатом вызова инструмента.
     * Используется в пользовательском сообщении после tool_use-ответа ассистента.
     *
     * @param type        всегда {@code "tool_result"}
     * @param tool_use_id идентификатор соответствующего tool_use-блока из ответа ассистента
     * @param content     текстовый результат вызова инструмента
     */
    public record ToolResultContent(String type, String tool_use_id, String content) {

        /**
         * Фабричный метод для удобного создания блока tool_result.
         *
         * @param toolUseId идентификатор tool_use-блока
         * @param result    строковый результат инструмента
         * @return готовый блок {@code tool_result}
         */
        public static ToolResultContent of(String toolUseId, String result) {
            return new ToolResultContent("tool_result", toolUseId, result);
        }
    }
}
