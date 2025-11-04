import { test, expect } from '@playwright/test';

test('play a complete game', async ({ page: playerOne, context }) => {
  let playerTwo = await context.newPage();
  await playerOne.goto('http://localhost:8000/');
  await playerTwo.goto('http://localhost:8000/');
  await playerOne.getByRole('textbox').click();
  await playerOne.getByRole('textbox').fill('abc');
  await playerOne.getByRole('button', { name: 'Start Game ABC' }).click();
  await playerTwo.getByRole('textbox').click();
  await playerTwo.getByRole('textbox').fill('abc');
  await playerTwo.getByRole('button', { name: 'Start Game ABC' }).click();

  await playerOne.locator('#cell-0').click();
  await expect(playerOne.locator('#cell-0'), "Should mark own field with X").toHaveText('X');
  await expect(playerTwo.locator('#cell-0'), "Should mark other player field with X").toHaveText('X');

  await playerTwo.locator('#cell-1').click();
  await playerOne.locator('#cell-3').click();
  await playerTwo.locator('#cell-2').click();
  await playerOne.locator('#cell-6').click();
  await expect(playerOne.locator('#endedbutton')).toBeVisible();
});
