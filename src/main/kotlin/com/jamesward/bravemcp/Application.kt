package com.jamesward.bravemcp

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import org.slf4j.LoggerFactory
import org.springaicommunity.mcp.annotation.McpTool
import org.springaicommunity.mcp.annotation.McpToolParam
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap


@SpringBootApplication
@EnableConfigurationProperties(LIProperties::class)
@RestController
class Application(val page: Page) {

    private val log = LoggerFactory.getLogger(Application::class.java)

    private val personCache = ConcurrentHashMap<String, String>()
    private val peopleSearchCache = ConcurrentHashMap<String, String>()
    private val companySearchCache = ConcurrentHashMap<String, String>()

    private fun cached(map: ConcurrentHashMap<String, String>, key: String, supplier: () -> String): String {
        val existing = map[key]
        if (existing != null) {
            return existing
        }
        val value = supplier()
        map[key] = value
        return value
    }

    @McpTool
    fun debugNavigate(path: String) {
        page.navigate("https://linkedin.com$path")
    }

    @McpTool(description = "get a person's profile")
    fun personProfile(
        @McpToolParam(description = "the username from https://linkedin.com/in/{username}") username: String
    ): String = cached(personCache, username.lowercase()) {
        page.navigate("https://linkedin.com/in/${username}")
        page.waitForLoadState()
        page.waitForSelector("#profile-content").innerText()
    }

    fun nameAndCompanyToString(name: String, company: String?) =
        company?.let {
            "$name $company"
        } ?: name

    @McpTool(description = "search for people")
    fun searchPeople(name: String, @McpToolParam(required = false) company: String?): String = cached(peopleSearchCache, nameAndCompanyToString(name, company).lowercase().trim()) {
        val query = nameAndCompanyToString(name, company).trim()
        page.navigate("https://www.linkedin.com/search/results/people/?keywords=$query&origin=SWITCH_SEARCH_VERTICAL&sid=!jD")
        page.waitForLoadState()
        val results = page.waitForSelector(".search-results-container")
        results.querySelectorAll("li").joinToString { item ->
            item.querySelectorAll("a").joinToString { it.getAttribute("href") } + "\n" + item.innerText()
        }
    }

    @McpTool(description = "search for companies")
    fun searchCompanies(name: String): String = cached(companySearchCache, name.lowercase()) {
        page.navigate("https://www.linkedin.com/search/results/companies/?keywords=$name&origin=GLOBAL_SEARCH_HEADER&sid=VX-")
        page.waitForLoadState()
        page.waitForSelector(".search-results-container").innerText()
    }

    @GetMapping("/current", produces = [MediaType.IMAGE_PNG_VALUE])
    fun currentPage(): ResponseEntity<ByteArray> = run {
        ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(page.screenshot())
    }

}

@Component
class LI(private val liProperties: LIProperties) {
    // todo: close

    @Bean
    fun browserPage(): Page = run {
        val playwright = Playwright.create()

//       val browser = playwright.chromium().connect("ws://localhost:3000")
        val launchOptions = BrowserType.LaunchOptions().setHeadless(liProperties.headless).setChromiumSandbox(false)
        val browser = playwright.chromium().launch(launchOptions)
        val page: Page = browser.newPage()

        page.navigate("https://www.linkedin.com/login")
        page.waitForLoadState()

        // Wait for login form to load
        page.waitForSelector("#username")
        // Fill in credentials
        page.fill("#username", liProperties.username)
        page.fill("#password", liProperties.password)
        // Submit the form
        page.click("button[type='submit']")

        //page.waitForURL("https://www.linkedin.com/feed/")

        page
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
