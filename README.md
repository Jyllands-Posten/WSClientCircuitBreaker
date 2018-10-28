# WSClientCircuitBreaker
Circuit breaker for the WSClient.   
When searching for at cirtuit breaker, we could use for the ws client provided with play, we could not find any suitable.
Hence we decided to build our own and publish it here.
For now we are still missing some documentation, example usage and tests.
We will try to privide these as time allows it - you are more than welcome to contribute these things or other things you see missing.
Please note that the cirtuit breaker itself is not very scala like, but rather java like, please help us improve if you can.

## The responsibility of the circuit breaker.
The circuit breaker is responsible for knowing the state of the service, you are trying to contact.
Some people might think that the circuit breaker is responsible for knowing what to do when the service is down - this is not the case.
The circuit breaker will inform you that the service is unreachable, by throwing a `CircuitBreakerOpenException`. You yourself is responsible for knowing what the right action is, it migth be one of the following (more options exist), these are just examples):
- Throw an exception 
- Do nothing, except log that the exception occoured
- Return a cached result


[gitter]:               https://gitter.im/Jyllands-Posten/WSClientCircuitBreaker
[gitter-badge]:         https://badges.gitter.im/Jyllands-Posten/WSClientCircuitBreaker.svg


## Community

You can join these groups and chats to discuss and ask WSClientCircuitBreaker related questions:

- Issue tracker: [![github: Jyllands-Posten/WSClientCircuitBreaker](https://img.shields.io/github/issues/Jyllands-Posten/WSClientCircuitBreaker.svg)](https://github.com/Jyllands-Posten/WSClientCircuitBreaker/issues)
- Chat room: [![gitter-badge][]][gitter]
 

## Contribution & Maintainers 

Contributions are welcomed. 

Refer to the [CONTRIBUTING.md](https://github.com/Jyllands-Posten/WSClientCircuitBreaker/blob/master/.github/CONTRIBUTING.md) file for more details about the workflow,
and general hints on how to prepare your pull request. You can also ask for clarifications or guidance in GitHub issues directly.

## License 

WSClientCircuitBreaker is Open Source and available under the Apache 2 License.
