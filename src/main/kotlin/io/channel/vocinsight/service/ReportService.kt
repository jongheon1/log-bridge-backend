package io.channel.vocinsight.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.channel.vocinsight.dto.PeriodInfo
import io.channel.vocinsight.dto.ReportDataRequest
import io.channel.vocinsight.dto.TagDataInfo
import io.channel.vocinsight.repository.ChatRepository
import io.channel.vocinsight.repository.TagRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class ReportService(
    private val chatRepository: ChatRepository,
    private val tagRepository: TagRepository,
    private val objectMapper: ObjectMapper
) {

    /**
     * 태그별 주간 리포트 데이터를 HTML 테이블로 생성
     * @param tagIds 태그 ID 목록
     * @param periodDays 주기 (데모용으로 7일 고정)
     * @param weeks 주차 개수 (기본 8주)
     */
    fun generateWeeklyReportHtml(tagIds: List<String>, periodDays: Int = 7, weeks: Int = 8): String {
        val now = LocalDateTime.now()
        val totalDays = periodDays * weeks
        val startDate = now.minusDays(totalDays.toLong())

        // 태그 정보 조회
        val tags = tagRepository.findAllById(tagIds)
        val tagMap = tags.associateBy { it.id }

        // 기간 내 모든 채팅 조회
        val allChats = chatRepository.findByChatCreatedAtBetween(startDate, now)

        // 주차별 날짜 범위 생성
        val weekRanges = generateWeekRanges(startDate.toLocalDate(), periodDays, weeks)

        // 태그별 주차별 데이터 집계
        val reportData = mutableMapOf<String, MutableList<Int>>()

        tagIds.forEach { tagId ->
            val tagName = tagMap[tagId]?.name
            if (tagName != null) {
                val weeklyCounts = mutableListOf<Int>()

                weekRanges.forEach { (weekStart, weekEnd) ->
                    val count = allChats.count { chat ->
                        val chatDate = chat.chatCreatedAt.toLocalDate()
                        val chatTagNames = chat.tagNames
                        chatDate in weekStart..weekEnd && chatTagNames.contains(tagName)
                    }
                    weeklyCounts.add(count)
                }

                reportData[tagName] = weeklyCounts
            }
        }

        // HTML 테이블 생성
        return generateHtmlTable(weekRanges, reportData)
    }

    /**
     * 주차별 날짜 범위 생성
     * @return List<Pair<시작일, 종료일>>
     */
    private fun generateWeekRanges(startDate: LocalDate, periodDays: Int, weeks: Int): List<Pair<LocalDate, LocalDate>> {
        val ranges = mutableListOf<Pair<LocalDate, LocalDate>>()
        var currentStart = startDate

        repeat(weeks) {
            val currentEnd = currentStart.plusDays(periodDays.toLong() - 1)
            ranges.add(currentStart to currentEnd)
            currentStart = currentEnd.plusDays(1)
        }

        return ranges
    }

    /**
     * HTML 테이블 생성
     */
    private fun generateHtmlTable(weekRanges: List<Pair<LocalDate, LocalDate>>, reportData: Map<String, List<Int>>): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val html = StringBuilder()
        html.append("<style>")
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }")
        html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: center; }")
        html.append("th { background-color: #4CAF50; color: white; font-weight: bold; }")
        html.append("tr:nth-child(even) { background-color: #f2f2f2; }")
        html.append("tr:hover { background-color: #ddd; }")
        html.append("</style>")

        html.append("<table>")
        html.append("<thead>")
        html.append("<tr>")
        html.append("<th>태그명</th>")

        // 헤더에 각 주차의 시작 날짜 추가
        weekRanges.forEach { (weekStart, _) ->
            html.append("<th>${weekStart.format(dateFormatter)}</th>")
        }

        html.append("</tr>")
        html.append("</thead>")
        html.append("<tbody>")

        // 각 태그별 데이터 행 추가
        reportData.forEach { (tagName, counts) ->
            html.append("<tr>")
            html.append("<td style='text-align: left; font-weight: bold;'>$tagName</td>")

            counts.forEach { count ->
                html.append("<td>$count</td>")
            }

            html.append("</tr>")
        }

        html.append("</tbody>")
        html.append("</table>")

        return html.toString()
    }

    /**
     * 8주 데이터를 AI 서버에 보낼 JSON 형식으로 생성
     * @param tagIds 태그 ID 목록
     * @param periodDays 주기 (일 단위)
     * @param weeks 주차 개수
     * @return JSON 문자열
     */
    fun generateWeeklyDataForAI(tagIds: List<String>, periodDays: Int, weeks: Int = 8): String {
        val now = LocalDateTime.now()
        val totalDays = periodDays * weeks
        val startDate = now.minusDays(totalDays.toLong())
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // 태그 정보 조회
        val tags = tagRepository.findAllById(tagIds)
        val tagMap = tags.associateBy { it.id }

        // 기간 내 모든 채팅 조회
        val allChats = chatRepository.findByChatCreatedAtBetween(startDate, now)

        // 주차별 날짜 범위 생성
        val weekRanges = generateWeekRanges(startDate.toLocalDate(), periodDays, weeks)

        // 주차별 데이터 집계
        val weeklyDataList = mutableListOf<io.channel.vocinsight.dto.WeeklyTagData>()
        var totalCount = 0

        weekRanges.forEachIndexed { index, (weekStart, weekEnd) ->
            val tagDataList = mutableListOf<TagDataInfo>()

            tagIds.forEach { tagId ->
                val tagName = tagMap[tagId]?.name
                if (tagName != null) {
                    val count = allChats.count { chat ->
                        val chatDate = chat.chatCreatedAt.toLocalDate()
                        val chatTagNames = chat.tagNames
                        chatDate in weekStart..weekEnd && chatTagNames.contains(tagName)
                    }
                    tagDataList.add(TagDataInfo(tagName = tagName, count = count))
                    totalCount += count
                }
            }

            weeklyDataList.add(
                io.channel.vocinsight.dto.WeeklyTagData(
                    weekNumber = index + 1,
                    weekStart = weekStart.format(dateFormatter),
                    weekEnd = weekEnd.format(dateFormatter),
                    tags = tagDataList
                )
            )
        }

        // 요약 정보 생성
        val summary = "최근 ${weeks}주간 총 ${totalCount}건의 VOC가 수집되었습니다."

        // ReportDataRequest 생성
        val reportData = ReportDataRequest(
            period = PeriodInfo(
                start = startDate.toLocalDate().format(dateFormatter),
                end = now.toLocalDate().format(dateFormatter),
                totalWeeks = weeks
            ),
            summary = summary,
            weeklyData = weeklyDataList
        )

        return objectMapper.writeValueAsString(reportData)
    }
}
