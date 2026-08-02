with open("app/src/main/java/com/example/ui/SuapViewModel.kt", "r") as f:
    text = f.read()

import re

new_func = """    private fun getDemoScheduleAndDiarios(): Pair<List<ScheduleItem>, List<DiarioItem>> {
        val sched = emptyList<ScheduleItem>()
        val diarios = emptyList<DiarioItem>()
        return Pair(sched, diarios)
    }"""

text = re.sub(r'    private fun getDemoScheduleAndDiarios\(\): Pair<List<ScheduleItem>, List<DiarioItem>> \{[\s\S]*?return Pair\(sched, diarios\)\n    \}', new_func, text)

with open("app/src/main/java/com/example/ui/SuapViewModel.kt", "w") as f:
    f.write(text)
