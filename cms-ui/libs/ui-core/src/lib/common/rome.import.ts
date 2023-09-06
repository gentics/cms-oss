/* eslint-disable @typescript-eslint/no-unsafe-call, @typescript-eslint/no-unsafe-assignment, @typescript-eslint/no-unsafe-member-access */
import * as rome_ from '@bevacqua/rome';
import * as momentum from '@bevacqua/rome/src/momentum';
import { getInstance } from './momentjs.import';

(window as any).moment = getInstance();
rome_.use(getInstance());
delete (window as any).moment;

if (momentum.moment === void 0) {
    throw new Error('rome depends on moment.js, you can get it at http://momentjs.com.');
}

/** The rome namespace and rome function. */
export const rome = (rome_ as any).default || rome_;
