package io.book.ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forward-контроллер для React-роутов SPA. React Router использует client-side routing,
 * поэтому при прямом обращении к {@code /agent}, {@code /rag-chat} или {@code /ollama-chat}
 * (например после обновления страницы или открытия по ссылке с телефона)
 * Spring обычно возвращает {@code 404}. Этот контроллер перенаправляет такие пути на
 * {@code /index.html} — дальше роутинг разруливает React в браузере.
 *
 * <p>API-эндпоинты {@code /api/**} обрабатываются обычными REST-контроллерами и сюда не попадают.
 */
@Controller
public class SpaForwardController {

    /**
     * Перенаправляет известные React-маршруты на {@code index.html}.
     *
     * @return forward-инструкция Spring MVC
     */
    @GetMapping(value = {
            "/agent",
            "/rag-chat",
            "/ollama-chat",
            "/agent/**",
            "/rag-chat/**",
            "/ollama-chat/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
