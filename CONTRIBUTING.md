### Contributing

Thanks for taking the interest in contributing to the project. Contributions are very welcome!

However, prior to writing any code, it would be better if you discussed with me what you want to do via issue, email (you'll find my email in `project.clj`) or Slack (`#ventas` in Clojurians). Doing otherwise might result in wasted or duplicated effort, which is never good.



General rules from the top of my head:

- PRs should target `dev` (as opposed to, say, `master`)
- We have few tests. I'd like them to keep passing (we have Travis watching this so don't worry too much about it).


- Don't make PRs with more than 50% of formatting changes on existing code, because I won't accept them.
- Do make PRs with well-defined scopes (fix _one_ bug, add _one_ feature...). For example, don't fix a bug in ElasticSearch indexing and add a payment method at the same time :)
- It's advisable to add a test for your changes, but don't feel pressured to do it.



If you wish to contribute but you don't know what do to, read this: [TODO.md](./TODO.md)