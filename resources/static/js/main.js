(function (global, Rx, Vue, VueRx, VueRouter) {

    $.ajaxSetup({
        crossDomain: false,
        beforeSend: function (xhr, settings) {
            if (!/^(GET|HEAD|OPTIONS|TRACE)$/.test(settings.type)) {
                xhr.setRequestHeader("x-xsrf-token", window.csrf);
            }
        }
    });


    Vue.use(VueRx, Rx);

    var empty = () => {};

    var listen = global.listen = function (endpoint) {
        return Rx.Observable.create(observer => {
            const eventSource = new EventSource(endpoint);
            eventSource.onmessage = x => observer.next(JSON.parse(x.data));
            eventSource.onerror = e => observer.error(e);
            return () => eventSource.close();
        }).catch(err => {
            console.log("Error received opening server sent events.", err);
            return Rx.Observable.empty();
        });
    };


    var crud = function (resource) {
        var collection = new Rx.BehaviorSubject({});
        var events = listen(`/api/subscribe/${resource}`);
        var subscription = events.subscribe(event => {
            collection.next((function () {
                var current = collection.getValue();
                var updates = {};
                var kind = event.event;
                delete event['event'];
                switch (kind) {
                    case "initial":
                        if (!updates[event.id]) {
                            updates[event.id] = event;
                        }
                        break;
                    case "create":
                        if (!updates[event.id]) {
                            updates[event.id] = event;
                        }
                        break;
                    case "update":
                        if (updates[event.id]) {
                            updates[event.id] = Object.assign({}, current[event.id], event);
                        }
                        break;
                    case "delete":
                        delete current[event.id];
                        break;
                    default:
                        console.log("Received event I don't know how to handle.");
                        break;
                }
                return Object.assign({}, current, updates);
            }()))
        });

        collection.subscribe(empty, empty, () => subscription.unsubscribe());

        return {
            events: function () {
                return events;
            },
            one: function (id) {
                return collection.map(db => db[id]);
            },
            all: function () {
                return collection.map(Object.values);
            },
            indexBy: function (by) {
                return collection.map(Object.values).map(items => {
                    return items.reduce(function (agg, el) {
                        agg[by(el)] = el;
                        return agg;
                    }, {});
                });
            },
            groupBy: function (by) {
                return collection.map(Object.values).map(items => {
                    return items.reduce(function (agg, el) {
                        var key = by(el);
                        if (key in agg) {
                            agg[key].push(el);
                        } else {
                            agg[key] = [el];
                        }
                        return agg;
                    }, {});
                });
            },
            transact: function (id, action) {
                $.ajax(`/api/${resource}/${id}`, {
                    data: JSON.stringify(action),
                    contentType: 'application/json',
                    type: 'POST',
                }).then(empty, alert);
            }
        }
    };

    var customers = global.customers = crud("customer");

}(window, Rx, Vue, VueRx, VueRouter));