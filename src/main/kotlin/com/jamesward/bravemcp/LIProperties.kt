package com.jamesward.bravemcp

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "li")
data class LIProperties(
    val username: String,
    val password: String,
)
