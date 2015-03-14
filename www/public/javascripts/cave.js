$(function () {

    $( document ).ready(function() {
        $('#issues').load("/status");
    });

    $("a.showToken").click(function () {
        $(this).siblings("span.tokenValue").show();
        $(this).remove();
    });

    $('#confirm-delete').on('show.bs.modal', function (e) {
        var email = $(e.relatedTarget).data('email');
        var org = $(e.relatedTarget).data('organization');
        var team = $(e.relatedTarget).data('team');

        $(this).find('input[name=email]').val(email);
        $(this).find('input[name=organization]').val(org);
        $(this).find('input[name=team]').val(team);

        var msg = $(this).find('p.confirmationMsg');
        var optionalTeam = "";
        if (team) {
            optionalTeam = team + " @ "
        }
        msg.text(msg.data("message") + " " + email + " from " + optionalTeam + org);
    });

    $('#deleteTokenModal').on('show.bs.modal', function (e) {
        var tokenId = $(e.relatedTarget).data('token-id');
        var org = $(e.relatedTarget).data('organization');
        var team = $(e.relatedTarget).data('team');
        var description = $(e.relatedTarget).data('description');

        $(this).find('input[name=tokenId]').val(tokenId);
        $(this).find('input[name=organization]').val(org);
        $(this).find('input[name=team]').val(team);

        $(this).find('.tokenDescription').text(description);
    });

    $('#change-role').on('show.bs.modal', function (e) {
        var email = $(e.relatedTarget).data('email');
        var org = $(e.relatedTarget).data('organization');
        var team = $(e.relatedTarget).data('team');
        var role = $(e.relatedTarget).data('role');
        var message = $(e.relatedTarget).data('message');
        var title = $(e.relatedTarget).data('title');

        $(this).find('input[name=email]').val(email);
        $(this).find('input[name=organization]').val(org);
        $(this).find('input[name=team]').val(team);
        $(this).find('select[name=role]').selectpicker('val', role);

        $(this).find('h4.modal-title').text(title);
        $(this).find('p.confirmationMsg').text(message);
    });

    var urlify = function (text) {
        var urlRegex = /(https?:\/\/[^\s]+)/g;
        return text.replace(urlRegex, function (url) {
            return '<a href="' + url + '">' + url + '</a>';
        })
    };

    $(".alertDescription").each(function () {
        var desc = $(this).text();
        $(this).text("");
        $(this).append(urlify(desc));
    });

    $('.modal').on('shown.bs.modal', function () {
        lastfocus = $(this);
        $(this).find('input:visible:first').focus();
    });

    var PleaseWait = PleaseWait || (function () {
        var pleaseWaitDiv = $("#pleaseWaitModal");
        return {
            show: function () {
                pleaseWaitDiv.modal();
            },
            hide: function () {
                pleaseWaitDiv.modal('hide');
            }
        };
    })();
    var ErrorDialog = ErrorDialog || (function () {
        var errorDiv = $("#errorModal");
        return {
            show: function (title, body, message) {
                $(errorDiv).find(".modal-body").empty().append('<h4>' + title + '</h4><p>' + body + '</p><p><br/>API message:<br/>' + message + '</p>');
                errorDiv.modal();
            },
            hide: function () {
                errorDiv.modal('hide');
            }
        };
    })();

    var GRAPH = {
        plot: function (seriesName, data, options) {
            $("#graphUrl").text(GRAPH.generateGraphUrl());
            window.history.pushState("", "", GRAPH.generateGraphUrl());

            var timestamps = ["x"];
            var values = [seriesName];
            var allNull = true;
            $.each(data, function () {
                timestamps.push(this.ts);
                values.push(this.value);

                if (this.value != null) allNull = false
            });

            if (allNull) {
                values = [];
                timestamps = [];
            }

            var yLabel = $("#aggregator").val() + "(" + metric + ")";
            var defaults = {
                bindto: '#chart',
                data: {
                    x: 'x',
                    xFormat: "%Y-%m-%dT%H:%M:%S.%LZ",
                    columns: [
                        timestamps,
                        values
                    ],
                    types: {},
                    empty: {
                        label: {
                            text: "No Data Available"
                        }
                    },
                    colors: {}
                },
                axis: {
                    x: {
                        type: 'timeseries',
                        tick: {
                            format: '%H:%M:%S                       %d %b %y',
                            fit: true,
                            multiline: true
                        },
                        width: 50
                    },
                    y: {
                        label: {
                            text: yLabel,
                            position: 'outer-middle'
                        }
                    }
                },
                subchart: {
                    show: true
                },
                legend: {
                    show: false
                },
                point: {
                    show: true
                }
            };

            defaults.data.types[seriesName] = 'area';
            defaults.data.colors[seriesName] = '#428bca';

            var settings = $.extend(true, {}, defaults, options);
            c3.generate(settings);
        },
        getTags: function () {
            var tags = [];
            $(".tagsSection input").each(function () {
                var val = $(this).val();
                if (val !== "") {
                    tags.push($(this).attr("name") + ':' + val);
                }
            });
            return tags.join();
        },
        getTimeRange: function () {
            var tr = $("#timeRange").find(".active input").val();
            if (tr === "custom") {
                return {
                    start: $('#startDate').data("DateTimePicker").getDate().toJSON(),
                    end: $('#endDate').data("DateTimePicker").getDate().toJSON()
                };
            } else {
                return {
                    start: moment().subtract(tr, 'second').toDate().toJSON(),
                    end: new Date().toJSON()
                };
            }
        },
        constructMetricDataUrl: function () {
            var interval = $("#interval").val();
            var aggregator = $("#aggregator").val();
            var timeRange = this.getTimeRange();
            var t = this.getTags();
            var tags = (t.length > 0) ? "&tags=" + t : "";

            if (team !== "")
                return "/organizations/" + organization + "/teams/" + team + "/metrics/data?metric=" + metric + tags +
                    "&period=" + interval + "&aggregator=" + aggregator + "&end=" + timeRange.end + "&start=" + timeRange.start;
            else
                return "/organizations/" + organization + "/metrics/data?metric=" + metric + tags +
                    "&period=" + interval + "&aggregator=" + aggregator + "&end=" + timeRange.end + "&start=" + timeRange.start;
        },
        constructUrlForTestAlertCondition: function () {
            var interval = $("#intervalForTest").val();
            var condition = encodeURIComponent($("#alertCondition").val());
            var timeRange = this.getTimeRange();

            if (team !== "")
                return "/organizations/" + organization + "/teams/" + team + "/metrics/check?metric=" + metric + "&condition=" + condition +
                    "&period=" + interval + "&end=" + timeRange.end + "&start=" + timeRange.start;
            else
                return "/organizations/" + organization + "/metrics/check?metric=" + metric + "&condition=" + condition +
                    "&period=" + interval + "&end=" + timeRange.end + "&start=" + timeRange.start;
        },

        plotMetric: function () {
            var $btn = $("#plot").button('loading');
            $(".autocomplete").each(function () {
                GRAPH.storeFieldValueForAutosuggestionBox($(this).val(), $(this).attr("id"));
            });
            PleaseWait.show();
            $.getJSON(GRAPH.constructMetricDataUrl(),function (data) {
                GRAPH.plot(metric, data);
            }).fail(function (jqXHR, textStatus, errorThrown) {
                    ErrorDialog.show('We are sorry, but we failed to retrieve your data.', 'You can try again, or you can raise a support ticket.', jqXHR.responseText);
                }).always(function () {
                    $btn.button('reset');
                    PleaseWait.hide();
                })
        },
        buildRegions: function(datapoins){
           var regions = [];
           var previous = null;
           var currentRegion = [];
           $.each(datapoins, function(index,datapoint){
               if(datapoint.value === 1) {
                   currentRegion.push(datapoint);
               } else if(currentRegion.length !== 0) {
                   regions.push(currentRegion);
                   currentRegion=[];
               }

               previous = datapoint;
           })

           if(currentRegion.length > 0) {
               regions.push(currentRegion)
           }

           for(var index in regions) {
               var region = regions[index];
               var newRegion = [];
               var startTs = region[0].ts;
               var endTs = region[region.length -1].ts
               var ts1 = moment(startTs).subtract(0.5, 'minute').toJSON();;
               var ts2 = moment(endTs).add(0.5, 'minute').toJSON();;
               regions[index] = {start: ts1, end: ts2, class:'alertRegion'};
           }

           return regions
        },
        plotConditionEvaluation: function () {
            var condition = $("#alertCondition").val();
            if (condition === "") return;

            var $btn = $("#evaluateCondition").button('loading');
            PleaseWait.show();
            $(".autocomplete").each(function () {
                GRAPH.storeFieldValueForAutosuggestionBox($(this).val(), $(this).attr("id"));
            });
            $.getJSON(GRAPH.constructMetricDataUrl(),function (data) {
                $.getJSON(GRAPH.constructUrlForTestAlertCondition(), function (conditionEvaluation) {
                    var customOptions = {
                        regions: GRAPH.buildRegions(conditionEvaluation)
                    };
                    GRAPH.plot(metric, data, customOptions);
                }).fail(function (jqXHR, textStatus, errorThrown) {
                        ErrorDialog.show('We are sorry, but we failed to retrieve your data.', 'You can try again, or you can raise a support ticket.', jqXHR.responseText);
                    }).always(function () {
                        $btn.button('reset');
                        PleaseWait.hide();
                    })
            }).fail(function (jqXHR, textStatus, errorThrown) {
                    $btn.button('reset');
                    PleaseWait.hide();
                    ErrorDialog.show('We are sorry, but we failed to evaluate your alert condition.', 'You can try again, or you can raise a support ticket.', jqXHR.responseText);
                })
        },
        storeFieldValueForAutosuggestionBox: function (value, fieldName) {
            var key = organization + "|" + team + "|" + metric + "|" + fieldName;
            var values = JSON.parse(localStorage.getItem(key) || "{}");
            values[value] = moment();
            localStorage.setItem(key, JSON.stringify(values));
        },

        initTimeRange: function () {
            var twoWeeksAgo = new Date(new Date().setDate(new Date().getDate()-14));
            $('#startDate, #startDateForTest').datetimepicker({
                sideBySide: true,
                minDate: twoWeeksAgo
            });
            $('#endDate, #endDateForTest').datetimepicker({
                sideBySide: true,
                minDate: startDate
            });
            // Initialize the start date with a max date of the current end date
            $('#startDate').data("DateTimePicker").setMaxDate($('#endDate').data("DateTimePicker").getDate());

            $("#startDate").on("dp.change", function (e) {
                $('#endDate').data("DateTimePicker").setMinDate(e.date);
            });

            $("#endDate").on("dp.change", function (e) {
                $('#startDate').data("DateTimePicker").setMaxDate(e.date);
            });

            $("#timeRange").find("label.btn-default").click(function () {
                if ($(this).find("#optionCustom").length > 0) {
                    $("#customDateRangePanel").show();
                    $('#startDate').data("DateTimePicker").setDate(moment().subtract(1, 'hour').toDate());
                    $('#endDate').data("DateTimePicker").setDate(moment().toDate());
                } else {
                    $("#customDateRangePanel").hide();
                }
            });
        },

        initAutosuggestionBoxes: function () {

            var substringMatcher = function () {
                return function findMatches(unescapedQuery, cb) {
                    var q = RegExp.escape(unescapedQuery);
                    var matches, substrRegex;

                    // an array that will be populated with substring matches
                    matches = [];
                    var fieldName = $(this.getRoot()).parent().parent().parent().find("input.autocomplete[id]").attr("id");
                    var key = organization + "|" + team + "|" + metric + "|" + fieldName;
                    var strs = [];
                    var values = JSON.parse(localStorage.getItem(key) || "{}");
                    for (var property in values) {
                        if (values.hasOwnProperty(property)) {
                            strs.push(property)
                        }
                    }

                    // regex used to determine if a string contains the substring `q`
                    substrRegex = new RegExp(q, 'i');

                    // iterate through the pool of strings and for any string that
                    // contains the substring `q`, add it to the `matches` array
                    $.each(strs, function (i, str) {
                        if (substrRegex.test(str)) {
                            // the typeahead jQuery plugin expects suggestions to a
                            // JavaScript object, refer to typeahead docs for more info
                            matches.push({
                                value: str
                            });
                        }
                    });

                    cb(matches);
                };
            };

            $('.autocomplete').typeahead({
                hint: true,
                highlight: true,
                minLength: 1
            }, {
                name: 'states',
                displayKey: 'value',
                source: substringMatcher()
            });
        },
        initNonEmptyFieldHighlight: function(){
            $('input').keyup(function(){
                if( $(this).val() == ""){
                    $(this).parents(".form-group").removeClass("has-success");
                }else{
                    $(this).parents(".form-group").addClass("has-success");
                }
            });
        },
        generateGraphUrl: function(){
            var period = $("#interval").val();
            var condition = $("#alertCondition").val();
            var intervalForTest = $("#intervalForTest").val();
            var aggregator = $("#aggregator").val();
            var timeRange = this.getTimeRange();
            var t = this.getTags();
            var tags = (t.length > 0) ? "&tags=" + t : "";
            return document.location.origin + document.location.pathname + "?period=" + period + "&aggregator=" +
                aggregator + tags + "&condition=" + condition + "&intervalForTest=" + intervalForTest +
                "&end=" + timeRange.end + "&start=" + timeRange.start;
        },
        initUserDefaults: function() {
            var caveFormDefaults = localStorage.getItem("caveFormDefaults");
            if ( caveFormDefaults !== null ) {
                try {
                    caveFormDefaults = JSON.parse(caveFormDefaults);
                } catch (e) {
                    console.log('Failure to parse caveFormDefaults');
                    return;
                }
                // get page name
                if ( caveFormDefaults[ $('.breadcrumb li:last').text() ] ) {
                    var pageDefaults = caveFormDefaults[ $('.breadcrumb li:last').text() ];
                    if (pageDefaults.aggregator) {
                        $('#aggregator').val(pageDefaults.aggregator)
                    }
                    if (pageDefaults.interval) {
                        $('#interval').val(pageDefaults.interval)
                    }
                }
            }
            // Now add click events to those form controls
            $('.user-default').change(function(e){
                // first time, caveFormDefaults === null, initialize the caveFormDefaults object
                // second time, caveFormDefaults exists
                var $button = $(e.currentTarget),
                    caveFormDefaults = localStorage.getItem("caveFormDefaults"),
                    $buttonVal = $button.data('toggle') === 'buttons' ?  $button.find('label.active input').val() : $button.val();
                if (caveFormDefaults === null) {
                    var caveFormDefaults = {};
                    caveFormDefaults[ $('.breadcrumb li:last').text() ] = {};
                    caveFormDefaults[ $('.breadcrumb li:last').text() ][ $button.attr('id') ] = $buttonVal;
                } else {
                    var newDefault = {};
                    newDefault[$('.breadcrumb li:last').text() ] = {};
                    newDefault[ $('.breadcrumb li:last').text() ][ $button.attr('id') ] = $buttonVal;
                    caveFormDefaults = $.extend(true, JSON.parse(caveFormDefaults), newDefault);
                }
                localStorage.setItem('caveFormDefaults', JSON.stringify(caveFormDefaults));
            });
        },
        init: function () {
            this.initAutosuggestionBoxes();
            this.initTimeRange();
            this.initNonEmptyFieldHighlight();
            this.initUserDefaults();

            $("#plot").click(this.plotMetric);
            $("#evaluateCondition").click(this.plotConditionEvaluation);

            $("#getGraphUrlModal").on("shown.bs.modal", function(){
                $("#graphUrl").select();
            });
            $(".graph #createAlertButton").click(function(){
                $("#condition").val($("#alertCondition").val());
                $("#period").val($("#intervalForTest :selected").attr("name"));
            });
            $("#alertCondition").keypress(function (e) {
                if (e.which == 13) {
                    $("#evaluateCondition").click();
                }
            });
            $(".tagsSection input").keypress(function (e) {
                if (e.which == 13) {
                    $("#plot").click();
                }
            });

            if($("#alertCondition").val()!== ""){
                this.plotConditionEvaluation();
            } else {
                this.plotMetric();
            }
        }
    };

    if($("#chart").length == 1) {
        GRAPH.init();
    }
    if ( $('.alerts').length ) {
        $('.alerts .list-group-item').click(function(e) {
        // If someone clicks an anchor tag within a list-group-item, don't propagate the event
            if (e.target.tagName.toUpperCase() === 'A' ) {
                e.stopPropagation();
            } else {
                window.location.assign($(e.currentTarget).data('rel'));
            }
        });
    }

    RegExp.escape = function(text) {
        return text.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&");
    };
});