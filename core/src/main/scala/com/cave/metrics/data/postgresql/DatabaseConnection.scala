package com.cave.metrics.data.postgresql

import com.cave.metrics.data.AwsConfig

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.apache.commons.logging.LogFactory

import scala.slick.driver.PostgresDriver.simple._


abstract class DatabaseConnection(awsConfig: AwsConfig) {

  val log = LogFactory.getLog(classOf[DatabaseConnection])
  val ds = new HikariDataSource(getDatabaseConfig)
  val db = {

    val database = Database.forDataSource(ds)
    log.debug( s"""
    Db connection initialized.
      driver: ${awsConfig.rdsJdbcDatabaseClass}
      user:   ${awsConfig.rdsJdbcDatabaseUser}
      pass:   [REDACTED]
    """.stripMargin)

    ds.getConnection.close()

    database
  }

  def closeDbConnection(): Unit = ds.close()

  private[this] def getDatabaseConfig: HikariConfig = {
    val config = new HikariConfig
    config.setMaximumPoolSize(awsConfig.rdsJdbcDatabasePoolSize)

    val className = awsConfig.rdsJdbcDatabaseClass
    config.setDataSourceClassName(awsConfig.rdsJdbcDatabaseClass)

    if (className.contains("postgres")) {
      config.addDataSourceProperty("serverName", awsConfig.rdsJdbcDatabaseServer)
      config.addDataSourceProperty("databaseName", awsConfig.rdsJdbcDatabaseName)
      config.addDataSourceProperty("portNumber", awsConfig.rdsJdbcDatabasePort)
    } else {
      config.addDataSourceProperty("url", awsConfig.rdsJdbcDatabaseUrl)
    }

    config.addDataSourceProperty("user", awsConfig.rdsJdbcDatabaseUser)
    config.addDataSourceProperty("password", awsConfig.rdsJdbcDatabasePassword)

    config
  }
}
