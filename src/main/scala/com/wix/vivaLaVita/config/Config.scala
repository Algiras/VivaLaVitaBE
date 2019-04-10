package com.wix.vivaLaVita.config

case class DBConfig(url: String, driver: String, keepAliveConnection: Boolean)
case class HttpConfig(host: String, port: Int)

case class Config(db: DBConfig, http: HttpConfig)