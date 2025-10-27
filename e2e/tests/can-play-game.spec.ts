import { test, expect } from '@playwright/test';

test('test', async ({ page, context }) => {
  let page2 = await context.newPage();
  await page.goto('http://localhost:8000/');
  await page2.goto('http://localhost:8000/');
  await page.getByRole('textbox').click();
  await page2.getByRole('textbox').click();
  await page.getByRole('textbox').fill('abc');
  await page2.getByRole('textbox').fill('abc');
  await page.getByRole('button', { name: 'Start Game ABC' }).click();
  await page2.getByRole('button', { name: 'Start Game ABC' }).click();

});