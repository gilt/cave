#!/usr/bin/env ruby
# == Wrapper script to update a local postgrseql database
#
# == Usage
#  ./dev.rb
#

command = "dropdb cavellc && createdb cavellc && sem-apply --host localhost --user vdumitrescu --name cavellc"
puts command
system(command)
