package com.seki999.kubecraft
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class AppTest {
 @get:Rule val rule = createAndroidComposeRule<MainActivity>()
 @Test fun homeAndLessonMap() {
  rule.waitUntil(15000) { rule.onAllNodesWithText("开始剧情课程").fetchSemanticsNodes().isNotEmpty() }
  rule.onNodeWithText("开始剧情课程").performClick()
  rule.onNodeWithText("01  点亮第一座工人站").assertIsDisplayed().performClick()
  rule.onNodeWithText("推进 1 tick").assertIsDisplayed()
 }
 @Test fun firstLessonAcceptsARealCommandAndCompletes() {
  rule.waitUntil(15000) { rule.onAllNodesWithText("开始剧情课程").fetchSemanticsNodes().isNotEmpty() }
  rule.onNodeWithText("开始剧情课程").performClick()
  rule.onNodeWithText("01  点亮第一座工人站").performClick()
  rule.onNodeWithText("终端").performClick()
  rule.onNodeWithText("kubectl / help").performTextInput("kubectl create node worker-1")
  rule.onNodeWithText("执行").performClick()
  val tasks = rule.onAllNodesWithText("任务")
  tasks[tasks.fetchSemanticsNodes().lastIndex].performClick()
  rule.onNodeWithText("验证任务并结算").performScrollTo().assertIsEnabled().performClick()
  rule.onNodeWithText("任务完成").assertIsDisplayed()
 }
 @Test fun settingsAccessible() {
  rule.waitUntil(15000) { rule.onAllNodesWithText("开始剧情课程").fetchSemanticsNodes().isNotEmpty() }
  rule.onNodeWithText("设置").performClick()
  rule.onNodeWithText("深色主题").assertIsDisplayed()
 }
}
