package io.channel.vocinsight.repository

import io.channel.vocinsight.domain.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamRepository : JpaRepository<Team, String>
