package com.cave.metrics.data

import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}

class PasswordHelperSpec extends FlatSpec with Matchers with BeforeAndAfter {

  var helper: PasswordHelper = _
  val password = "S0me_S3cr3t"
  val knownHash = "$2a$10$bQScHjdHCMidJ8yM5VF35umKdWt1dzIWRrxAFLuK2i3EBIXWqAa4a"

  before {
    helper = new PasswordHelper
  }

  "PasswordHelper" should "generate hash and salt for a password" in {
    val password = "S0me_S3cr3t"
    val info = helper.encryptPassword(password)

    info.salt.isDefined should be(true)
    info.hash.startsWith(info.salt.get) should be(true)
  }

  it should "validate a password from a known hash" in {
    helper.matchPassword(password, knownHash, None) should be (true)
  }
}
