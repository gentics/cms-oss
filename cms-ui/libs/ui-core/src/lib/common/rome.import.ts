/* eslint-disable @typescript-eslint/no-unsafe-call */
import { getInstance } from './momentjs.import';
import * as romeCore from '@bevacqua/rome';

romeCore.use(getInstance());

export const rome = romeCore;
