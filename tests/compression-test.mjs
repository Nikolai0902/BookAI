/**
 * BookAI — тест сжатия контекста (30 ходов)
 *
 * Установка:  npm install playwright
 * Запуск:     node tests/compression-test.mjs
 *
 * Требования: фронтенд запущен на http://127.0.0.1:5173
 *             бэкенд запущен на http://localhost:8080
 */

import { chromium } from 'playwright';
import { writeFileSync, mkdirSync } from 'fs';

const APP_URL        = 'http://127.0.0.1:5173/agent';
const SCREENSHOTS_DIR = './result/playwright';
const RESPONSE_TIMEOUT = 120_000; // 2 мин на один ответ модели

// 30 вопросов — реалистичная книжная беседа
const QUESTIONS = [
  'Привет! Меня зовут Алекс',
  'Как меня зовут?',
  'Посоветуй 5 классических романов',
  'Расскажи подробнее про первый из них',
  'Кто написал эту книгу и когда?',
  'Какие ещё книги у этого автора?',
  'Расскажи про вторую книгу из твоего списка',
  'Есть ли экранизация этой книги?',
  'Посоветуй что-то в жанре фэнтези',
  'Расскажи подробнее про первую из рекомендаций',
  'Есть ли продолжение у этой книги?',
  'Сколько книг в этой серии?',
  'Расскажи про третью книгу серии',
  'Кто главный герой этой серии?',
  'Посоветуй детективы',
  'Расскажи про Агату Кристи',
  'Какая её самая известная книга?',
  'Расскажи про Эркюля Пуаро',
  'Есть ли сериал по книгам Агаты Кристи?',
  'Посоветуй современную фантастику',
  'Расскажи про первую книгу из списка подробнее',
  'Есть ли продолжение у неё?',
  'Посоветуй книги про историю',
  'Расскажи про историю Древнего Рима',
  'Какие книги про Рим посоветуешь?',
  'Расскажи подробнее про первую из них',
  'Ты помнишь как меня зовут?',
  'Что такое нарративная проза?',
  'Посоветуй книги в этом жанре',
  'Подведи итог нашего разговора о книгах',
];

// ──────────────────────────────────────────────────────────────
// Парсинг статистики из сайдбара
// ──────────────────────────────────────────────────────────────
async function extractStats(page) {
  return page.evaluate(() => {
    function findValueAfterLabel(labelText) {
      const spans = Array.from(document.querySelectorAll('aside span'));
      for (let i = 0; i < spans.length - 1; i++) {
        if (spans[i].textContent?.trim() === labelText) {
          return spans[i + 1].textContent?.trim() ?? null;
        }
      }
      return null;
    }

    const parseNum = (s) =>
      parseInt((s ?? '0').replace(/[\s,\u00a0\u2009]/g, ''), 10) || 0;
    const parseF = (s) => parseFloat(s ?? '0') || 0;

    return {
      turns:         parseNum(findValueAfterLabel('Ходов')),
      newMessage:    parseNum(findValueAfterLabel('Новое сообщение ≈')),
      historyTokens: parseNum(findValueAfterLabel('История (весь контекст)')),
      totalInput:    parseNum(findValueAfterLabel('Оплачено input')),
      totalOutput:   parseNum(findValueAfterLabel('Оплачено output')),
      cost:          parseF(findValueAfterLabel('Стоимость сессии $')),
      recentAsIs:    parseNum(findValueAfterLabel('Как есть')),
      summarized:    parseNum(findValueAfterLabel('В саммари')),
    };
  });
}

// ──────────────────────────────────────────────────────────────
// Отправка одного сообщения + ожидание ответа через network
// ──────────────────────────────────────────────────────────────
async function sendMessage(page, message) {
  const textarea = page.locator('textarea[placeholder*="Напишите"]');
  await textarea.fill(message);

  const responsePromise = page.waitForResponse(
    (resp) =>
      resp.url().includes('/api/agent/chat') &&
      resp.request().method() === 'POST',
    { timeout: RESPONSE_TIMEOUT },
  );

  await textarea.press('Enter');
  const resp = await responsePromise;
  const data = await resp.json();

  // Небольшая пауза для обновления React-состояния
  await page.waitForTimeout(400);
  return data;
}

// ──────────────────────────────────────────────────────────────
// Полная сессия: настройка → 30 вопросов → скриншот → статистика
// ──────────────────────────────────────────────────────────────
async function runSession(page, useCompression) {
  const label = useCompression ? 'СО СЖАТИЕМ' : 'БЕЗ СЖАТИЯ';
  console.log(`\n${'═'.repeat(55)}`);
  console.log(`  Сессия: ${label}`);
  console.log('═'.repeat(55));

  await page.goto(APP_URL, { waitUntil: 'networkidle' });

  // Выбрать Haiku — самая быстрая и дешёвая модель
  await page.selectOption('select', 'claude-haiku-4-5-20251001');

  // Задать сжатие (чекбокс активен только до первого сообщения)
  const checkbox = page.locator('input[type="checkbox"]');
  const isChecked = await checkbox.isChecked();
  if (useCompression !== isChecked) await checkbox.click();

  console.log(`  Модель: Haiku | Сжатие: ${useCompression ? 'ВКЛ ✓' : 'ВЫКЛ'}`);
  console.log(`  Отправляем ${QUESTIONS.length} сообщений...\n`);

  const turnStats = [];

  for (let i = 0; i < QUESTIONS.length; i++) {
    const q = QUESTIONS[i];
    const prefix = `  [${String(i + 1).padStart(2)}/${QUESTIONS.length}]`;
    process.stdout.write(`${prefix} ${q.substring(0, 38).padEnd(40)} `);

    try {
      const data = await sendMessage(page, q);
      const economy = turnStats.length > 0
        ? ` (Δ+${data.inputTokens - (turnStats.at(-1)?.inputTokens ?? 0)})`
        : '';
      process.stdout.write(`✓  in:${String(data.inputTokens).padStart(5)}  out:${String(data.outputTokens).padStart(4)}${economy}\n`);
      turnStats.push({
        turn:        i + 1,
        inputTokens: data.inputTokens,
        outputTokens: data.outputTokens,
      });
    } catch (err) {
      process.stdout.write(`✗  ОШИБКА: ${err.message}\n`);
    }
  }

  // Скриншот финального состояния
  mkdirSync(SCREENSHOTS_DIR, { recursive: true });
  const file = `${SCREENSHOTS_DIR}/${useCompression ? 'with' : 'without'}-compression.png`;
  await page.screenshot({ path: file });
  console.log(`\n  Скриншот: ${file}`);

  const stats = await extractStats(page);
  stats.turnStats = turnStats;
  return stats;
}

// ──────────────────────────────────────────────────────────────
// Вывод итоговой таблицы сравнения
// ──────────────────────────────────────────────────────────────
function printComparison(without, withComp) {
  const pct = (a, b) => {
    if (!a) return '    —';
    const v = ((b - a) / a * 100).toFixed(1);
    return (b > a ? '+' : '') + v + '%';
  };

  console.log('\n\n' + '═'.repeat(70));
  console.log('  ИТОГ: сравнение 30 ходов');
  console.log('═'.repeat(70));

  const rows = [
    ['Ходов',                      without.turns,         withComp.turns],
    ['История — последний запрос',  without.historyTokens, withComp.historyTokens],
    ['Оплачено input (итого)',      without.totalInput,    withComp.totalInput],
    ['Оплачено output (итого)',     without.totalOutput,   withComp.totalOutput],
    ['Стоимость сессии, $',         without.cost,          withComp.cost],
  ];

  console.log(
    `  ${'Метрика'.padEnd(32)} ${'Без сжатия'.padEnd(14)} ${'Со сжатием'.padEnd(14)} Δ`,
  );
  console.log('  ' + '─'.repeat(66));

  for (const [label, a, b] of rows) {
    console.log(
      `  ${label.padEnd(32)} ${String(a).padEnd(14)} ${String(b).padEnd(14)} ${pct(a, b)}`,
    );
  }

  if (withComp.recentAsIs || withComp.summarized) {
    console.log(
      `\n  Контекст (финал): ${withComp.recentAsIs} сообщ. как есть + ${withComp.summarized} в саммари`,
    );
  }

  // Рост контекста по ходам — каждый 5-й
  console.log('\n  --- Рост input-токенов по ходам (каждый 5-й) ---');
  console.log(
    `  ${'Ход'.padEnd(5)} ${'Без сжатия'.padEnd(14)} ${'Со сжатием'.padEnd(14)} Экономия`,
  );
  const maxTurns = Math.max(without.turnStats.length, withComp.turnStats.length);
  for (let i = 0; i < maxTurns; i += 5) {
    const a = without.turnStats[i]?.inputTokens ?? 0;
    const b = withComp.turnStats[i]?.inputTokens ?? 0;
    const save = a > 0 ? ((a - b) / a * 100).toFixed(1) + '%' : '—';
    console.log(
      `  ${String(i + 1).padEnd(5)} ${String(a).padEnd(14)} ${String(b).padEnd(14)} ${save}`,
    );
  }

  // Сохранить JSON
  const result = {
    timestamp: new Date().toISOString(),
    turns:     QUESTIONS.length,
    without,
    with:      withComp,
  };
  const jsonPath = `${SCREENSHOTS_DIR}/comparison.json`;
  writeFileSync(jsonPath, JSON.stringify(result, null, 2));
  console.log(`\n  JSON: ${jsonPath}`);
  console.log('═'.repeat(70));
}

// ──────────────────────────────────────────────────────────────
// Точка входа
// ──────────────────────────────────────────────────────────────
async function main() {
  console.log('BookAI — тест сжатия контекста');
  console.log(`URL: ${APP_URL}`);
  console.log('Убедитесь, что фронтенд и бэкенд запущены!\n');

  const browser = await chromium.launch({ headless: false, slowMo: 30 });
  const page = await browser.newPage();
  page.setDefaultTimeout(RESPONSE_TIMEOUT);

  try {
    const statsWithout = await runSession(page, false);
    const statsWith    = await runSession(page, true);
    printComparison(statsWithout, statsWith);
  } finally {
    await browser.close();
  }
}

main().catch((err) => {
  console.error('Ошибка:', err);
  process.exit(1);
});
