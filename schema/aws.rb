#!/usr/bin/env ruby
# == Wrapper script to update AWS postgrseql database
#
# == Usage
#  ./aws.rb
#

command = "sem-apply --host cavellc.cewqzb4ffa71.us-east-1.rds.amazonaws.com --user cavellc --name cavellc"
puts command
system(command)
